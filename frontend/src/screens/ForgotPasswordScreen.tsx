import { useMutation } from "@tanstack/react-query";
import { useState } from "react";

import { BASE_API_URL } from "@/config/app-query-client";
import styles from "./ForgotPasswordScreen.module.css";

export function ForgotPasswordScreen() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const recoverMutation = useMutation({
    mutationFn: async (email: string) => {
      const response = await fetch(`${BASE_API_URL}/sessions/recover`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Frontend-URL": window.location.origin,
        },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Error al enviar recuperación");
      }
    },
    onSuccess: () => {
      setSent(true);
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message);
      setSent(false);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    recoverMutation.mutate(email);
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Recuperar contraseña</h2>

      <form onSubmit={handleSubmit}>
        <input
          type="email"
          placeholder="Tu email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={styles.input}
        />

        <button type="submit" disabled={recoverMutation.isPending}>
          Enviar
        </button>
      </form>

      {sent && <p className={styles.success}>Si el email existe, recibirás instrucciones.</p>}

      {error && <p className={styles.error}>{error}</p>}
    </div>
  );
}
