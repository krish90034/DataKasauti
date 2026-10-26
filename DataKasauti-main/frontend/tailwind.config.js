/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        datakasauti: {
          teal: "#0F766E",
          deepTeal: "#115E59",
          beige: "#F3EBDD",
          cream: "#FFFCF6",
          mint: "#E0F0E9",
          border: "#E3D8C6",
          text: "#253532",
          muted: "#728078",
        },
      },
    },
  },
  plugins: [],
};
