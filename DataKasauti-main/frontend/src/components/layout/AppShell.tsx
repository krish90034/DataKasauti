import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import {
  Activity,
  ArrowLeftRight,
  FileSpreadsheet,
  LayoutDashboard,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Wifi,
  WifiOff,
} from "lucide-react";

import { checkBackendHealth } from "../../services/healthService";

type BackendStatus = "checking" | "connected" | "unavailable";

const navigation = [
  {
    label: "Dashboard",
    path: "/",
    icon: LayoutDashboard,
    caption: "Overview and upload",
  },
  {
    label: "Data Profile",
    path: "/profile",
    icon: FileSpreadsheet,
    caption: "Quality insights",
  },
  {
    label: "Compare Data",
    path: "/compare",
    icon: ArrowLeftRight,
    caption: "Dataset differences",
  },
  {
    label: "Data Cleaning",
    path: "/clean",
    icon: Sparkles,
    caption: "Preview and export",
  },
];

const routeTitles: Record<string, string> = {
  "/": "Dashboard",
  "/profile": "Data Profile",
  "/compare": "Compare Data",
  "/clean": "Data Cleaning",
};

export default function AppShell() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>("checking");
  const [backendVersion, setBackendVersion] = useState("");
  const [retryToken, setRetryToken] = useState(0);
  const location = useLocation();

  useEffect(() => {
    let cancelled = false;

    async function loadBackendStatus() {
      setBackendStatus("checking");
      try {
        const result = await checkBackendHealth();
        if (cancelled) return;
        if (result.status.toUpperCase() === "UP") {
          setBackendStatus("connected");
          setBackendVersion(result.version);
        } else {
          setBackendStatus("unavailable");
          setBackendVersion("");
        }
      } catch {
        if (cancelled) return;
        setBackendStatus("unavailable");
        setBackendVersion("");
      }
    }

    void loadBackendStatus();
    return () => {
      cancelled = true;
    };
  }, [retryToken]);

  const statusConfig = {
    checking: {
      label: "Checking backend",
      className: "bg-amber-50 text-amber-800",
      icon: RefreshCw,
    },
    connected: {
      label: "Backend connected",
      className: "bg-[#DDF1E8] text-[#115E59]",
      icon: Wifi,
    },
    unavailable: {
      label: "Backend unavailable",
      className: "bg-rose-50 text-rose-800",
      icon: WifiOff,
    },
  }[backendStatus];
  const StatusIcon = statusConfig.icon;
  const pageTitle = routeTitles[location.pathname] ?? "Data Quality Workspace";

  return (
    <div className="min-h-screen bg-[#F4EDE0] text-[#253532] md:flex">
      <aside className="z-20 w-full shrink-0 border-b border-[#0A4945] bg-[#0B403D] text-white md:sticky md:top-0 md:h-screen md:w-[258px] md:overflow-y-auto md:border-b-0 md:border-r md:border-white/5">
        <div className="flex items-center justify-between gap-3 px-5 pb-5 pt-5 md:block md:px-5 md:pb-7 md:pt-7">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#DDF1E8] text-[#0F766E] shadow-sm shadow-black/10">
              <ShieldCheck size={23} strokeWidth={2.2} />
            </div>
            <div>
              <h1 className="text-[17px] font-extrabold tracking-tight text-white">
                DataKasauti
              </h1>
              <p className="mt-0.5 text-[11px] tracking-wide text-[#B9D8CC]">
                DATA QUALITY PLATFORM
              </p>
            </div>
          </div>
          <span className="rounded-full border border-white/15 bg-white/5 px-3 py-1 text-[10px] font-semibold uppercase tracking-wider text-[#CFE6DC] md:hidden">
            MVP
          </span>
        </div>

        <div className="hidden px-5 pb-2 text-[10px] font-bold uppercase tracking-[0.18em] text-[#76A99A] md:block">
          Workspace
        </div>
        <nav
          aria-label="Main navigation"
          className="flex gap-2 overflow-x-auto px-3 pb-3 md:flex-col md:px-3 md:pb-4"
        >
          {navigation.map(({ label, path, icon: Icon, caption }) => (
            <NavLink
              key={path}
              to={path}
              end={path === "/"}
              className={({ isActive }) =>
                `group relative flex min-w-fit shrink-0 items-center gap-3 rounded-xl px-3 py-3 text-left transition duration-200 focus:outline-none focus:ring-2 focus:ring-[#C1E9D9] md:min-w-0 ${
                  isActive
                    ? "bg-[#0F766E] text-white shadow-[0_8px_18px_rgba(0,0,0,0.12)]"
                    : "text-[#D1E6DE] hover:bg-white/[0.07] hover:text-white"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition ${isActive ? "bg-white/15 text-white" : "bg-white/[0.05] text-[#A8CFC1] group-hover:bg-white/10 group-hover:text-white"}`}
                  >
                    <Icon size={18} strokeWidth={2} />
                  </span>
                  <span className="min-w-0">
                    <span className="block whitespace-nowrap text-[13px] font-semibold">
                      {label}
                    </span>
                    <span
                      className={`mt-0.5 hidden text-[10px] md:block ${isActive ? "text-[#D5F0E5]" : "text-[#8DB7A8]"}`}
                    >
                      {caption}
                    </span>
                  </span>
                  {isActive && (
                    <span className="absolute bottom-3 left-0 top-3 w-[3px] rounded-r-full bg-[#DDF1E8]" />
                  )}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="hidden px-4 md:mt-5 md:block">
          <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-white/[0.09] to-white/[0.025] p-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#DDF1E8]/10 text-[#BDE7D6]">
              <Activity size={18} />
            </div>
            <p className="mt-3 text-sm font-bold text-white">
              Data you can trust
            </p>
            <p className="mt-1.5 text-xs leading-5 text-[#B5D3C8]">
              Find quality issues early. Review every cleaning change before
              export.
            </p>
            <div className="mt-4 flex items-center gap-2 border-t border-white/10 pt-3 text-[10px] text-[#8DB7A8]">
              <span className="h-1.5 w-1.5 rounded-full bg-[#80C8AC]" /> Built
              for careful data work
            </div>
          </div>
        </div>
        <div className="hidden px-5 pb-5 pt-6 text-[10px] text-[#679789] md:block">
          DATAKASAUTI · MVP WORKSPACE
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-10 flex min-h-[68px] flex-wrap items-center justify-between gap-3 border-b border-[#E5D9C7] bg-[#FFFCF6]/95 px-4 py-3 backdrop-blur-md sm:px-6 lg:px-8">
          <div className="min-w-0">
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#8A938A]">
              Workspace <span className="px-1 text-[#C4B8A4]">/</span> Data
              Quality
            </p>
            <p className="mt-1 text-sm font-bold text-[#253532]">{pageTitle}</p>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            <div
              role="status"
              aria-live="polite"
              className={`flex items-center gap-2 rounded-full border border-transparent px-3 py-2 text-[11px] font-semibold ${statusConfig.className}`}
            >
              <StatusIcon
                size={14}
                className={backendStatus === "checking" ? "animate-spin" : ""}
              />
              <span>{statusConfig.label}</span>
              {backendStatus === "connected" && backendVersion && (
                <span className="hidden border-l border-current/20 pl-2 opacity-75 sm:inline">
                  v{backendVersion}
                </span>
              )}
            </div>
            {backendStatus === "unavailable" && (
              <button
                type="button"
                onClick={() => setRetryToken((value) => value + 1)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#E3D8C6] bg-white px-3 py-2 text-[11px] font-semibold text-[#465A54] transition hover:border-[#B7D8CC] hover:bg-[#F4F8F2]"
              >
                <RefreshCw size={13} />{" "}
                <span className="hidden sm:inline">Retry</span>
              </button>
            )}
          </div>
        </header>
        <main className="min-w-0 px-4 py-5 sm:px-6 sm:py-6 lg:px-8 lg:py-7">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
