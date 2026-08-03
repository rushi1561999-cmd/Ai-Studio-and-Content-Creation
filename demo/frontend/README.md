# AI Studio Frontend

React 19 and Vite client for AI Studio. It uses `/api` by default and the development server proxies that path to `http://localhost:8081`.

```bash
npm ci
npm run lint
npm run build
npm run dev
```

Set `VITE_API_URL` only when the API is hosted at a different origin. Authentication tokens are attached by the Axios interceptor; a 401 clears the local session. Razorpay Checkout is loaded from its official hosted script and is used only when the backend reports a complete gateway configuration.
