import { useState } from "react";

import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { useAuthenticatedFetch } from "@/services/TokenContext";

import styles from "./AdminScreen.module.css";

import { UserPublicDTO } from "./admin/types";
import { Avatar } from "../components/Avatar/Avatar";
import { useUserSearch } from "./admin/useUserSearch";
import { ProfesionalModal } from "./admin/ProfesionalModal";
import { ClienteModal } from "./admin/ClienteModal";

export const AdminScreen = () => {
  const authedFetch = useAuthenticatedFetch();

  const profSearch = useUserSearch("PROFESIONAL");
  const clienteSearch = useUserSearch("CLIENTE");

  const [profSeleccionado, setProfSeleccionado] = useState<UserPublicDTO | null>(null);
  const [clienteSeleccionado, setClienteSeleccionado] = useState<UserPublicDTO | null>(null);

  const handleProfDeleted = () => {
    setProfSeleccionado(null);
    profSearch.setQuery("");
    profSearch.setResults([]);
  };

  const handleClienteDeleted = () => {
    setClienteSeleccionado(null);
    clienteSearch.setQuery("");
    clienteSearch.setResults([]);
  };

  return (
    <CommonLayout>
      <div className={styles.dashboard}>
        <div className={styles.col}>
          <section className={styles.card}>
            <h2 className={styles.sectionTitle}>Profesionales</h2>
            <div className={styles.searchWrapper}>
              <input
                className={styles.searchInput}
                type="text"
                placeholder="🔍 Buscar por nombre o email..."
                value={profSearch.query}
                onChange={(e) => profSearch.setQuery(e.target.value)}
              />
              {profSearch.loading && <p className={styles.hint}>Buscando...</p>}
              {profSearch.results.length > 0 && (
                <ul className={styles.dropdown}>
                  {profSearch.results.map((p) => (
                    <li
                      key={p.id}
                      className={styles.dropdownItem}
                      onClick={() => {
                        setProfSeleccionado(p);
                        profSearch.setResults([]);
                      }}
                    >
                      <Avatar src={p.fotoPerfil} nombre={p.nombre} apellido={p.apellido} size={32} />
                      <div>
                        <div>
                          {p.nombre} {p.apellido}
                        </div>
                        <div className={styles.emailHint}>{p.email}</div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            {profSearch.query.trim().length > 0 && profSearch.query.trim().length < 2 && (
              <p className={styles.hint}>Escribí al menos 2 caracteres para buscar</p>
            )}
          </section>
        </div>

        <div className={styles.col}>
          <section className={styles.card}>
            <h2 className={styles.sectionTitle}>Clientes</h2>
            <div className={styles.searchWrapper}>
              <input
                className={styles.searchInput}
                type="text"
                placeholder="🔍 Buscar por nombre o email..."
                value={clienteSearch.query}
                onChange={(e) => clienteSearch.setQuery(e.target.value)}
              />
              {clienteSearch.loading && <p className={styles.hint}>Buscando...</p>}
              {clienteSearch.results.length > 0 && (
                <ul className={styles.dropdown}>
                  {clienteSearch.results.map((c) => (
                    <li
                      key={c.id}
                      className={styles.dropdownItem}
                      onClick={() => {
                        setClienteSeleccionado(c);
                        clienteSearch.setResults([]);
                      }}
                    >
                      <Avatar src={c.fotoPerfil} nombre={c.nombre} apellido={c.apellido} size={32} />
                      <div>
                        <div>
                          {c.nombre} {c.apellido}
                        </div>
                        <div className={styles.emailHint}>{c.email}</div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            {clienteSearch.query.trim().length > 0 && clienteSearch.query.trim().length < 2 && (
              <p className={styles.hint}>Escribí al menos 2 caracteres para buscar</p>
            )}
          </section>
        </div>
      </div>

      {profSeleccionado && (
        <ProfesionalModal
          userId={profSeleccionado.id}
          onClose={() => setProfSeleccionado(null)}
          onDeleted={handleProfDeleted}
          authedFetch={authedFetch}
        />
      )}
      {clienteSeleccionado && (
        <ClienteModal
          userId={clienteSeleccionado.id}
          onClose={() => setClienteSeleccionado(null)}
          onDeleted={handleClienteDeleted}
          authedFetch={authedFetch}
        />
      )}
    </CommonLayout>
  );
};
