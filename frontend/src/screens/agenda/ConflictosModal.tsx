import { formatFecha } from "../../utils/formatters";
import styles from "../AgendaScreen.module.css";
import { Avatar } from "../../components/Avatar/Avatar";

type TurnoConflictivo = {
  id: number;
  clienteNombre: string | null;
  nombreCliente: string | null;
  clienteApellido: string | null;
  clienteFotoPerfil: string | null;
  fecha: string;
  bloqueHorario: string;
};

type Props = {
  turnos: TurnoConflictivo[];
  savingAgenda: boolean;
  onCancelar: () => void;
  onMantener: () => void;
};

export const ConflictosModal = ({ turnos, savingAgenda, onCancelar, onMantener }: Props) => {
  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <h3 className={styles.modalTitle}>Turnos afectados por el cambio</h3>
        <p className={styles.modalText}>
          Los siguientes turnos quedan fuera del nuevo horario. ¿Qué querés hacer con ellos?
        </p>
        <ul className={styles.conflictosList}>
          {turnos.map((t) => (
            <li key={t.id} className={styles.conflictoItem}>
              <Avatar src={t.clienteFotoPerfil} nombre={t.clienteNombre ?? t.nombreCliente} apellido={t.clienteApellido} size={28} />
              <span>
                <strong>{t.clienteNombre ?? t.nombreCliente ?? "—"} {t.clienteApellido ?? ""}</strong>
                {" — "}
                {formatFecha(t.fecha)} a las {t.bloqueHorario}hs
              </span>
            </li>
          ))}
        </ul>
        <div className={styles.modalActions}>
          <button className={styles.btnCancelarTurnos} onClick={onCancelar} disabled={savingAgenda}>
            Cancelar turnos afectados
          </button>
          <button className={styles.btnMantener} onClick={onMantener} disabled={savingAgenda}>
            Mantener los turnos
          </button>
        </div>
      </div>
    </div>
  );
};
