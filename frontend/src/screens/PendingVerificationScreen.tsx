import { useState } from "react";
import { BASE_API_URL } from "@/config/app-query-client";
import { useToken, useAuthenticatedFetch } from "@/services/TokenContext";
import { useRefresh } from "@/services/UserServices";
import styles from "./PendingVerificationScreen.module.css";

export function PendingVerificationScreen() {
  const [resendStatus, setResendStatus] = useState<"idle" | "sending" | "sent" | "error">("idle");
  const [, setTokenState] = useToken();
  const authedFetch = useAuthenticatedFetch();
  const { mutate: refresh, isPending: isRefreshing } = useRefresh();

  const handleResend = async () => {
    if (resendStatus === "sending" || resendStatus === "sent") return;
    setResendStatus("sending");
    try {
      const meRes = await authedFetch(`${BASE_API_URL}/users/me`);
      if (!meRes.ok) throw new Error();
      const { email } = await meRes.json();
      const res = await authedFetch(`${BASE_API_URL}/sessions/resend-verification`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      setResendStatus(res.ok ? "sent" : "error");
    } catch {
      setResendStatus("error");
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.icon}>📧</div>

        <div>
          <h1 className={styles.title}>
            Verificá tu cuenta
          </h1>
          <p className={styles.description}>
            Te enviamos un correo de verificación. Hacé clic en el enlace para activar tu cuenta y empezar a usar el sistema.
          </p>
        </div>

        <div className={styles.alert}>
          No podrás usar el sistema hasta verificar tu correo electrónico.
        </div>

        <div className={styles.actions}>
          <button
            onClick={() => refresh()}
            disabled={isRefreshing}
            className={styles.btnPrimary}
          >
            {isRefreshing ? "Verificando..." : "Ya verifiqué mi cuenta / el admin me habilitó"}
          </button>

          {resendStatus === "sent" ? (
            <p className={styles.successText}>
              Email reenviado. Revisá tu bandeja de entrada.
            </p>
          ) : resendStatus === "error" ? (
            <p className={styles.errorText}>
              No se pudo reenviar el email. Intentá de nuevo.
            </p>
          ) : (
            <button
              onClick={handleResend}
              disabled={resendStatus === "sending"}
              className={styles.btnOutline}
            >
              {resendStatus === "sending" ? "Enviando..." : "Reenviar email de verificación"}
            </button>
          )}

          <button
            onClick={() => setTokenState({ state: "LOGGED_OUT" })}
            className={styles.btnGhost}
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  );
}
