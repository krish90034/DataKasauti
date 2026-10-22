import { NavLink, Outlet } from "react-router-dom";
import {
  Activity,
  ArrowLeftRight,
  FileSpreadsheet,
  LayoutDashboard,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

const navigation = [
  { label: "Dashboard", path: "/", icon: LayoutDashboard },
  { label: "Data Profile", path: "/profile", icon: FileSpreadsheet },
  { label: "Compare Data", path: "/compare", icon: ArrowLeftRight },
  { label: "Data Cleaning", path: "/clean", icon: Sparkles },
];

export default function AppShell() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 md:flex">
      <aside className="w-full border-b border-slate-200 bg-white p-5 md:min-h-screen md:w-64 md:border-b-0 md:border-r">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-700 text-white">
            <ShieldCheck size={23} />
          </div>

          <div>
            <h1 className="text-lg font-bold tracking-tight">DataKasauti</h1>
            <p className="text-xs text-slate-500">Data Quality Platform</p>
          </div>
        </div>

        <nav className="mt-8 flex gap-2 overflow-x-auto md:flex-col">
          {navigation.map(({ label, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              end={path === "/"}
              className={({ isActive }) =>
                `flex shrink-0 items-center gap-3 rounded-lg px-3 py-3 text-sm font-medium transition ${
                  isActive
                    ? "bg-teal-50 text-teal-800"
                    : "text-slate-600 hover:bg-slate-100"
                }`
              }
            >
              <Icon size={19} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-8 hidden rounded-xl bg-slate-50 p-4 md:block">
          <Activity size={20} className="text-teal-700" />
          <p className="mt-2 text-sm font-semibold">Data you can trust</p>
          <p className="mt-1 text-xs leading-5 text-slate-500">
            Detect quality issues before they affect your analysis.
          </p>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-5 md:px-8">
          <p className="text-sm text-slate-500">Workspace / Data Quality</p>

          <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-800">
            MVP Preview
          </span>
        </header>

        <main className="p-5 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
