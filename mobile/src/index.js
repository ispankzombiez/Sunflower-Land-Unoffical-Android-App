import { Web3Wallet } from '@walletconnect/web3wallet';
import { SignClient } from '@walletconnect/sign-client';
import { Core } from '@walletconnect/core';
import { notificationService } from './notificationService.js';

const PROJECT_ID = process.env.WC_PROJECT_ID || 'YOUR_PROJECT_ID';
const SUNFLOWER_URL = 'https://play.sunflower-land.com';

let web3wallet = null;
let signClient = null;

// Intercept all link opening attempts to route Binance links to native app
const originalOpen = window.open;
window.open = function(url, name, specs) {
    console.log('window.open called with URL:', url);
    // Route bnc:// and binance:// schemes through native bridge
    if (url && (url.startsWith('bnc://') || url.startsWith('binance://'))) {
        console.log('Routing Binance link through native bridge:', url);
        window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(url);
        return null;
    }
    return originalOpen.apply(this, arguments);
};

// Also intercept window.location.href assignments
Object.defineProperty(window, 'location', {
    value: new Proxy(window.location, {
        set: function(target, property, value) {
            if (property === 'href' && value && (value.startsWith('bnc://') || value.startsWith('binance://'))) {
                console.log('Intercepted binance link via window.location.href:', value);
                window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(value);
                return true;
            }
            target[property] = value;
            return true;
        }
    }),
    writable: true
});

// Handle deep links from mobile wallets
window.addEventListener('walletDeepLink', async (event) => {
    console.log('Received wallet deep link:', event.detail);
    try {
        // Handle MetaMask and other wallet links
        const url = new URL(event.detail);
        console.log('Parsed deep link:', { 
            scheme: url.protocol, 
            channelId: url.searchParams.get('channelId'),
            version: url.searchParams.get('v')
        });
        // Forward to iframe
        const gameFrame = document.getElementById('gameFrame');
        if (gameFrame && gameFrame.contentWindow) {
            gameFrame.contentWindow.postMessage({ type: 'WALLET_LINK', url: event.detail }, '*');
        }
    } catch (error) {
        console.error('Error handling wallet deep link:', error);
    }
});

async function initializeWalletConnect() {
  try {
    const core = new Core({
      projectId: PROJECT_ID
    });

    signClient = await SignClient.init({
      projectId: PROJECT_ID,
      metadata: {
        name: 'Sunflower Land Mobile',
        description: 'Sunflower Land Mobile App',
        url: window.location.origin,
        icons: ['https://walletconnect.com/walletconnect-logo.png']
      }
    });

    web3wallet = await Web3Wallet.init({
      core,
      metadata: {
        name: 'Sunflower Land Mobile',
        description: 'Sunflower Land Mobile App',
        url: window.location.origin,
        icons: ['https://walletconnect.com/walletconnect-logo.png']
      }
    });

    window.ethereum = {
      isMetaMask: false,
      isWalletConnect: true,
      request: async ({ method, params }) => {
        try {
          const session = signClient.session.getAll()[0];
          if (!session) throw new Error('No active session');

          const response = await signClient.request({
            topic: session.topic,
            chainId: `eip155:1`,
            request: {
              method,
              params
            }
          });

          return response;
        } catch (error) {
          console.error('WalletConnect request error:', error);
          throw error;
        }
      }
    };

    // Subscribe to session proposals
    signClient.on('session_proposal', async (proposal) => {
      try {
        const { id, params } = proposal;
        const { requiredNamespaces, relays } = params;

        const approvedNamespaces = {};
        Object.keys(requiredNamespaces).forEach((key) => {
          approvedNamespaces[key] = {
            chains: requiredNamespaces[key].chains,
            methods: requiredNamespaces[key].methods,
            events: requiredNamespaces[key].events,
            accounts: [`eip155:1:0x...`] // Replace with actual wallet address
          };
        });

        const session = await signClient.approve({
          id,
          namespaces: approvedNamespaces
        });

        if (session) {
          // Debug: We're already at the game URL, no need to navigate
          console.log('[DEBUG] WalletConnect session approved');
        }
      } catch (err) {
        console.error('Failed to approve session:', err);
        await signClient.reject({ id: proposal.id, reason: { code: 0, message: 'User rejected.' } });
      }
    });

  } catch (err) {
    console.error('Failed to initialize WalletConnect:', err);
    document.getElementById('connectOverlay').classList.add('active');
  }
}

function handleWalletConnectDeepLink(uri) {
  try {
    // Parse the URI and handle the connection
    console.log('Received WalletConnect URI:', uri);
    web3wallet.pair({ uri });
  } catch (err) {
    console.error('Failed to handle WalletConnect deep link:', err);
  }
}

document.getElementById('connectWallet').addEventListener('click', async () => {
  try {
    const uri = await web3wallet.core.pairing.create();
    // Create deep link URL for MetaMask Mobile
    const encodedUri = encodeURIComponent(uri);
    const deepLink = `metamask://wc?uri=${encodedUri}`;
    
    // Open the deep link
    window.location.href = deepLink;
    
    console.log('Opening MetaMask with URI:', uri);
  } catch (err) {
    console.error('Failed to create pairing:', err);
  }
});


// Listen for deep link events from Android
window.addEventListener('walletconnectDeepLink', (event) => {
  handleWalletConnectDeepLink(event.detail);
});

// Initialize notification service FIRST before navigating
(async () => {
  try {
    console.log('🚀 App starting - requesting notification permissions...');
    await notificationService.initialize();
    console.log('✅ Notification service initialized successfully');
  } catch (error) {
    console.error('❌ Failed to initialize notification service:', error);
  }
  
  // THEN navigate to the game
  console.log('🎮 Loading Sunflower Land...');
  window.location.href = SUNFLOWER_URL;
})();

// Commented out for direct load test: window.addEventListener('load', initializeWalletConnect);

