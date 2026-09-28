import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "wouter";
import { BASE_API_URL } from "@/config/app-query-client";
import styles from "./CambioPasswordLogueado.module.css";

export function ResetPasswordScreen() {
  const params = new URLSearchParams(window.location.search);
  const tokenParam = params.get("token");
  const [newPassword, setNewPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [success, setSuccess] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const passwordPolicy = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

  const resetMutation = useMutation({
    mutationFn: async () => {
      const response = await fetch(`${BASE_API_URL}/sessions/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: tokenParam, newPassword }),
      });
      if (!response.ok) {
        const json = await response.json().catch(() => ({}));
        throw new Error(json.error || "Error al restablecer la contraseña");
      }
    },
    onSuccess: () => {
      setSuccess(true);
      setServerError(null);
    },
    onError: (err: Error) => {
      setServerError(err.message);
    },
  });

  const newError = newPassword && !passwordPolicy.test(newPassword)
    ? "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
    : "";
  const repeatError = repeatPassword && repeatPassword !== newPassword
    ? "Las contraseñas no coinciden"
    : "";

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (newError || repeatError || !newPassword || !repeatPassword) return;
    if (!tokenParam) {
      setServerError("Token de recuperación inválido");
      return;
    }
    resetMutation.mutate();
  };

  if (!tokenParam) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h1 className={styles.title}>Enlace inválido</h1>
          <p>El enlace de recuperación no es válido. Solicita uno nuevo.</p>
          <Link href="/forgot-password" className="button">
            Solicitar recuperación
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Restablecer contraseña</h1>
        {success ? (
          <>
            <div className={styles.success}>Contraseña restablecida correctamente</div>
            <Link href="/login" className="button">
              Iniciar sesión
            </Link>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            {serverError && <div className={styles.error}>{serverError}</div>}

            <div className={styles.field}>
              <label className="label">Nueva contraseña</label>
              <input
                type="password"
                className={`input ${newError ? styles.inputError : ""}`}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
              {newError && <div className={styles.error}>{newError}</div>}
            </div>

            <div className={styles.field}>
              <label className="label">Repetir contraseña</label>
              <input
                type="password"
                className={`input ${repeatError ? styles.inputError : ""}`}
                value={repeatPassword}
                onChange={(e) => setRepeatPassword(e.target.value)}
                required
              />
              {repeatError && <div className={styles.error}>{repeatError}</div>}
            </div>

            <div className={styles.actions}>
              <button type="submit" className="button" disabled={resetMutation.isPending}>
                {resetMutation.isPending ? "Cargando..." : "Restablecer contraseña"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
