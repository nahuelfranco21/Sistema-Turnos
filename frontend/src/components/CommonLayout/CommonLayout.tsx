import React from "react";
import { Link, useLocation } from "wouter";

import { ErrorBoundary } from "@/components/ErrorBoundary/ErrorBoundary";
import { useToken } from "@/services/TokenContext";

import styles from "./CommonLayout.module.css";

export const CommonLayout = ({
  children,
}: React.PropsWithChildren) => {
  const [tokenState] = useToken();

  return (
    <div className={styles.mainLayout}>
      <ul className={styles.topBar}>
        <li className={styles.homeItem}>
          <Link
            href="/"
            aria-label="Inicio"
          >
            <span className={styles.logo}>TurnosYa</span>
          </Link>
        </li>

        <li style={{ flex: 1 }} />

        {tokenState.state ===
        "LOGGED_OUT" ? (
          <LoggedOutLinks />
        ) : (
          <LoggedInLinks />
        )}
      </ul>

      <div className={styles.body}>
        <ErrorBoundary>
          {children}
        </ErrorBoundary>
      </div>
    </div>
  );
};

const LoggedOutLinks = () => {
  return (
    <>
      <li>
        <Link href="/login">
          Log in
        </Link>
      </li>

      <li>
        <Link href="/signup">
          Sign Up
        </Link>
      </li>
    </>
  );
};

const LoggedInLinks = () => {
  const [tokenState, setTokenState] =
    useToken();

  const [location] =
    useLocation();

  const logOut = () => {
    setTokenState({
      state: "LOGGED_OUT",
    });
  };

  const roles =
    tokenState.state ===
    "LOGGED_IN"
      ? tokenState.tokens.roles
      : [];

  const isProfesional =
    roles.includes(
      "PROFESIONAL"
    );

  const isSuperAdmin =
    roles.includes(
      "SUPER_ADMIN"
    );

  const onAgenda =
    location === "/agenda";

  const onProfesionales =
    location ===
    "/profesionales";

  return (
    <>
      {isSuperAdmin ? (
        <li>
          <Link href="/admin">
            Panel admin
          </Link>
        </li>
      ) : isProfesional ? (
        <>
          <li>
            <Link href="/servicios">
              Mis servicios
            </Link>
          </li>

          <li>
            <Link href="/agenda">
              Mi agenda
            </Link>
          </li>

          {onProfesionales && (
            <li>
              <Link href="/agenda">
                <button>
                  Cambiar a
                  Profesional
                </button>
              </Link>
            </li>
          )}

          {onAgenda && (
            <li>
              <Link href="/profesionales">
                <button>
                  Cambiar a vista
                  cliente
                </button>
              </Link>
            </li>
          )}
        </>
      ) : null}

      <li>
        <Link
          href="/perfil"
          aria-label="Perfil"
        >
          <span
            style={{
              display:
                "inline-flex",
              alignItems:
                "center",
              gap: "0.5rem",
            }}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle
                cx="12"
                cy="7"
                r="4"
              ></circle>
            </svg>

            <span>
              Mi Perfil
            </span>
          </span>
        </Link>
      </li>

      <li>
        <button
          onClick={logOut}
        >
          Cerrar sesión
        </button>
      </li>
    </>
  );
};