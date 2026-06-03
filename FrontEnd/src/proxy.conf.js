const PROXY_CONFIG = {
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "pathRewrite": {
      "^/api": ""
    },
    "logLevel": "debug",
    "onProxyReq": function(proxyReq, req, res) {
      console.log('Proxying request:', req.method, req.url, '-> http://localhost:8080' + req.url.replace('/api', ''));
    },
    "onError": function(err, req, res) {
      console.error('Proxy error:', err);
    }
  }
};

module.exports = PROXY_CONFIG;
