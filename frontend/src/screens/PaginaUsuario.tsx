import { useEffect, useState } from "react";
import { Link } from "wouter";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";
import { COLORS } from "../constants";

import { Row } from "./perfil/Row";
import { FotoPerfil } from "./perfil/FotoPerfil";
import { DescripcionEditor } from "./perfil/DescripcionEditor";
import { UbicacionEditor } from "./perfil/UbicacionEditor";

type UserProfileDTO = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  role: string;
  fechaNacimiento: string | null;
  profesion: string | null;
  sector: string | null;
  ubicacion: string | null;
  fotoPerfil: string | null;
  descripcion: string | null;
  verified: boolean;
};

const ROLE_LABEL: Record<string, string> = {
  CLIENTE: "Cliente",
  PROFESIONAL: "Profesional",
  SUPER_ADMIN: "Administrador",
};

export const PaginaUsuario = () => {
  const authedFetch = useAuthenticatedFetch();
  const [profile, setProfile] = useState<UserProfileDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const res = await authedFetch(`${BASE_API_URL}/users/me`);
        if (!active) return;
        if (!res.ok) throw new Error(`Error ${res.status}`);
        setProfile(await res.json());
      } catch (e: unknown) {
        if (active) setError(e instanceof Error ? e.message : "Error al cargar el perfil");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  return (
    <CommonLayout>
      <main style={{ padding: 32, maxWidth: 480, margin: "0 auto" }}>
        <button
          onClick={() => window.history.back()}
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            color: COLORS.primary,
            fontWeight: 600,
            fontSize: 14,
            display: "flex",
            alignItems: "center",
            gap: 6,
            marginBottom: 24,
            padding: 0,
          }}
        >
          ← Volver al dashboard
        </button>

        <h1 style={{ marginBottom: 24 }}>Mi perfil</h1>

        {loading && <p style={{ color: COLORS.mutedGray }}>Cargando...</p>}
        {error && <p style={{ color: COLORS.error }}>{error}</p>}

        {profile && (
          <div
            style={{
              background: "var(--surface)",
              borderRadius: 12,
              padding: 28,
              boxShadow: "0 2px 12px rgba(0,0,0,0.08)",
              display: "flex",
              flexDirection: "column",
              gap: 16,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 16, marginBottom: 8 }}>
              <FotoPerfil
                fotoPerfil={profile.fotoPerfil}
                nombre={profile.nombre}
                apellido={profile.apellido}
                onFotoUpdate={(base64) => setProfile((prev) => (prev ? { ...prev, fotoPerfil: base64 } : prev))}
              />

              <div>
                <div style={{ fontWeight: 700, fontSize: 18 }}>
                  {profile.nombre} {profile.apellido}
                </div>
                <div
                  style={{
                    display: "inline-block",
                    marginTop: 4,
                    background: "#ede9fe",
                    color: "#5b21b6",
                    borderRadius: 20,
                    padding: "2px 10px",
                    fontSize: 12,
                    fontWeight: 600,
                  }}
                >
                  {ROLE_LABEL[profile.role] ?? profile.role}
                </div>
              </div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <span style={{ fontSize: 11, fontWeight: 600, color: "var(--muted)", textTransform: "uppercase", letterSpacing: 0.5 }}>
                  Email
                </span>
                <Link
                  href="/cambiar-email-logueado"
                  style={{ color: COLORS.primary, fontWeight: 600, fontSize: 13, textDecoration: "none", cursor: "pointer" }}
                >
                  Editar
                </Link>
              </div>
              <span style={{ fontSize: 15, color: "var(--text)" }}>{profile.email}</span>
            </div>

            <Row label="Fecha de nacimiento" value={profile.fechaNacimiento ?? "—"} />

            <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <span style={{ fontSize: 11, fontWeight: 600, color: "var(--muted)", textTransform: "uppercase", letterSpacing: 0.5 }}>
                  Contraseña
                </span>
                <Link
                  href="/cambiar-password-logueado"
                  style={{ color: COLORS.primary, fontWeight: 600, fontSize: 13, textDecoration: "none", cursor: "pointer" }}
                >
                  Editar
                </Link>
              </div>
              <span style={{ fontSize: 15, color: "var(--text)" }}>{"•".repeat(12)}</span>
            </div>

            {profile.role !== "CLIENTE" && (
              <>
                <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span style={{ fontSize: 11, fontWeight: 600, color: "var(--muted)", textTransform: "uppercase", letterSpacing: 0.5 }}>
                      Profesión
                    </span>
                    <Link
                      href="/cambiar-profesion-logueado"
                      style={{ color: COLORS.primary, fontWeight: 600, fontSize: 13, textDecoration: "none", cursor: "pointer" }}
                    >
                      Editar
                    </Link>
                  </div>
                  <span style={{ fontSize: 15, color: "var(--text)" }}>
                    {profile.profesion ? profile.profesion.charAt(0).toUpperCase() + profile.profesion.slice(1) : "—"}
                  </span>
                </div>

                <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span style={{ fontSize: 11, fontWeight: 600, color: "var(--muted)", textTransform: "uppercase", letterSpacing: 0.5 }}>
                      Sector
                    </span>
                    <Link
                      href="/cambiar-profesion-logueado"
                      style={{ color: COLORS.primary, fontWeight: 600, fontSize: 13, textDecoration: "none", cursor: "pointer" }}
                    >
                      Editar
                    </Link>
                  </div>
                  <span style={{ fontSize: 15, color: "var(--text)" }}>
                    {profile.sector
                      ? profile.sector.toLowerCase().replace(/_/g, " ").replace(/^\w/, (c) => c.toUpperCase())
                      : "—"}
                  </span>
                </div>
              </>
            )}

            {profile.role === "PROFESIONAL" && (
              <UbicacionEditor
                ubicacion={profile.ubicacion}
                profesion={profile.profesion}
                sector={profile.sector}
                onUbicacionUpdate={(ubicacion) => setProfile((prev) => (prev ? { ...prev, ubicacion } : prev))}
              />
            )}

            {profile.role !== "CLIENTE" && (
              <DescripcionEditor
                descripcion={profile.descripcion}
                onDescripcionUpdate={(descripcion) => setProfile((prev) => (prev ? { ...prev, descripcion } : prev))}
              />
            )}
          </div>
        )}
      </main>
    </CommonLayout>
  );
};

export default PaginaUsuario;
