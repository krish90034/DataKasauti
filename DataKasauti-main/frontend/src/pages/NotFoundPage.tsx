import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-xl rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-10 text-center">
      <h1 className="text-3xl font-bold text-[#253532]">404</h1>
      <p className="mt-2 text-[#728078]">Page not found.</p>
      <Link
        to="/"
        className="mt-4 inline-flex rounded-lg bg-[#0F766E] px-4 py-2.5 font-semibold text-white transition hover:bg-[#115E59]"
      >
        Return to DataKasauti
      </Link>
    </div>
  );
}
