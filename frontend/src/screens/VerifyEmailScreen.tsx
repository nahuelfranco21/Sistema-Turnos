import { useEffect, useState } from "react";
import { Link } from "wouter";
import { BASE_API_URL } from "@/config/app-query-client";
import { useToken } from "@/services/TokenContext";
import { useRefresh } from "@/services/UserServices";
import styles from "./CambioPasswordLogueado.module.css";

export function VerifyEmailScreen() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");

  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [errorMsg, setErrorMsg] = useState("");

  const [tokenState] = useToken();
  const { mutate: refresh } = useRefresh();

  useEffect(() => {
    if (!token) {
      setStatus("error");
      setErrorMsg("El enlace de verificación no es válido.");
      return;
    }

    fetch(`${BASE_API_URL}/sessions/verify?token=${encodeURIComponent(token)}`)
      .then(async (res) => {
        if (res.ok) {
          setStatus("success");
          if (tokenState.state === "LOGGED_IN") {
            refresh();
          }
        } else {
          const json = await res.json().catch(() => ({}));
          setErrorMsg(json.error ?? "Token inválido o expirado.");
          setStatus("error");
        }
      })
      .catch(() => {
        setErrorMsg("No se pudo conectar con el servidor.");
        setStatus("error");
      });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const homeHref = tokenState.state === "LOGGED_IN" ? "/" : "/login";
  const homeLabel = tokenState.state === "LOGGED_IN" ? "Ir al inicio" : "Iniciar sesión";

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        {status === "loading" && (
          <p className={styles.title}>Verificando tu cuenta...</p>
        )}

        {status === "success" && (
          <>
            <h1 className={styles.title}>¡Cuenta verificada!</h1>
            <div className={styles.success}>
              Tu email fue verificado correctamente.
            </div>
            <div className={styles.actions}>
              <Link href={homeHref} className="button">
                {homeLabel}
              </Link>
            </div>
          </>
        )}

        {status === "error" && (
          <>
            <h1 className={styles.title}>Enlace inválido</h1>
            <div className={styles.error}>{errorMsg}</div>
            <div className={styles.actions}>
              <Link href={homeHref} className="button">
                {homeLabel}
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
