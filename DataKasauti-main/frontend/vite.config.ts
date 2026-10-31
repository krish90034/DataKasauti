import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],

  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              // Keep the heavy Plotly package out of the initial application bundle.
              // It is requested when a lazily loaded page needs a chart.
              name: "plotly",
              test: /node_modules[\\/](?:plotly\.js)(?:[\\/]|$)/,
              priority: 20,
              minSize: 100 * 1024,
            },
            {
              // Keep React and React Router code in a reusable vendor chunk.
              name: "react-vendor",
              test: /node_modules[\\/](?:react|react-dom|react-router|react-router-dom)(?:[\\/]|$)/,
              priority: 10,
              minSize: 20 * 1024,
            },
          ],
        },
      },
    },
  },

  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    clearMocks: true,
    restoreMocks: true,
  },
});
