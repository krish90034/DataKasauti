import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="p-10 text-center">
      <h1 className="text-3xl font-bold">404</h1>
      <p className="mt-2 text-slate-500">Page not found.</p>
      <Link to="/" className="mt-4 inline-block text-teal-700 underline">
        Return to DataKasauti
      </Link>
    </div>
  );
}
