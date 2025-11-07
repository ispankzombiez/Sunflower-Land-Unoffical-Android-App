// EIP-1193 shim to expose a minimal window.ethereum that forwards to window.injectedProvider
(function() {
  if (window.ethereum && window.ethereum.isInjectedShim) return;

  function safeCall(fn) {
    try { return fn(); } catch (e) { console.error('shim safeCall', e); }
  }

  const shim = {
    isInjectedShim: true,
    request: function(args) {
      if (!window.injectedProvider) return Promise.reject(new Error('No injectedProvider available'));
      if (typeof args === 'string') return window.injectedProvider.request({ method: args, params: [] });
      return window.injectedProvider.request(args);
    },
    enable: function() { return this.request({ method: 'eth_requestAccounts' }); },
    connect: function() { return this.enable(); },
    disconnect: function() {
      if (window.injectedProvider && typeof window.injectedProvider.disconnect === 'function') return window.injectedProvider.disconnect();
      return Promise.resolve();
    },
    send: function(methodOrPayload, params) {
      if (!window.injectedProvider) throw new Error('No injectedProvider');
      if (typeof methodOrPayload === 'object') return window.injectedProvider.request(methodOrPayload);
      return window.injectedProvider.request({ method: methodOrPayload, params: params || [] });
    },
    sendAsync: function(payload, cb) {
      this.request(payload).then(res => cb(null, res)).catch(err => cb(err));
    },
    on: function(eventName, handler) {
      if (!window.injectedProvider || typeof window.injectedProvider.on !== 'function') return;
      const map = { accountsChanged: 'accountsChanged', chainChanged: 'chainChanged', connect: 'connect', disconnect: 'disconnect' };
      const mapped = map[eventName] || eventName;
      window.injectedProvider.on(mapped, handler);
    },
    removeListener: function(eventName, handler) {
      if (!window.injectedProvider || typeof window.injectedProvider.removeListener !== 'function') return;
      const map = { accountsChanged: 'accountsChanged', chainChanged: 'chainChanged', connect: 'connect', disconnect: 'disconnect' };
      const mapped = map[eventName] || eventName;
      window.injectedProvider.removeListener(mapped, handler);
    },
    isConnected: function() { return !!(window.injectedProvider && window.injectedProvider.connected); }
  };

  // When injectedProvider becomes available later, forward events to window.ethereum and set selectedAddress
  function attachWhenReady() {
    if (window.injectedProvider) {
      try {
        // sync selectedAddress / accounts
        if (window.injectedProvider.accounts && window.injectedProvider.accounts.length) {
          try { Object.defineProperty(shim, 'selectedAddress', { get: () => window.injectedProvider.accounts[0] }); } catch(e) {}
        }
        // forward events
        if (typeof window.injectedProvider.on === 'function') {
          window.injectedProvider.on('accountsChanged', (accounts) => {
            try { window.dispatchEvent(new CustomEvent('accountsChanged', { detail: accounts })); } catch(e){}
          });
          window.injectedProvider.on('chainChanged', (chainId) => {
            try { window.dispatchEvent(new CustomEvent('chainChanged', { detail: chainId })); } catch(e){}
          });
        }
      } catch (e) { console.warn('attachWhenReady error', e); }
      // expose shim
      safeCall(() => { window.ethereum = shim; window.dispatchEvent(new Event('ethereum#initialized')); });
    } else {
      // retry a few times
      setTimeout(attachWhenReady, 200);
    }
  }

  // Immediately expose a provisional shim that will route once injectedProvider is present
  window.ethereum = shim;
  setTimeout(attachWhenReady, 50);
})();
