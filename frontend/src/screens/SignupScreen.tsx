import { CommonLayout } from "@/components/CommonLayout/CommonLayout";
import { useAppForm } from "@/config/use-app-form";
import { SignupRequestSchema } from "@/models/Login";
import { useSignup } from "@/services/UserServices";
import { SECTORES } from "../constants";

import styles from "./Signup.module.css";

export const SignupScreen = () => {
  const { mutate, error } = useSignup();

  const formData = useAppForm({
    defaultValues: {
      email: "",
      password: "",
      nombre: "",
      apellido: "",
      fechaNacimiento: "",
      esProfesional: false,
      profesion: null as string | null,
      sector: null as string | null,
      ubicacion: null as string | null,
    },
    validators: {
      onChange: SignupRequestSchema,
    },
    onSubmit: async ({ value }) => mutate(value),
  });

  return (
    <CommonLayout>
      <div className={styles.signupContainer}>
        <div className={styles.signupCard}>
          <span className={styles.logo}>TurnosYa</span>
          <h1 className={styles.title}>Registro</h1>

          <formData.AppForm>
            <formData.FormContainer extraError={error}>
              <formData.AppField
                  name="email"
                  children={(field) => <field.TextField label="Email" />}
              />
              <formData.AppField
                  name="password"
                  children={(field) => <field.PasswordField label="Contraseña" />}
              />
              <formData.AppField
                  name="nombre"
                  children={(field) => <field.TextField label="Nombre" />}
              />
              <formData.AppField
                  name="apellido"
                  children={(field) => <field.TextField label="Apellido" />}
              />
              <formData.AppField
                  name="fechaNacimiento"
                  children={(field) => <field.TextField label="Fecha de nacimiento" type="date" />}
              />

              <formData.AppField
                  name="esProfesional"
                  children={(field) => (
                      <label className={styles.checkbox}>
                        <input
                            type="checkbox"
                            checked={field.state.value as boolean}
                            onChange={(e) => field.handleChange(e.target.checked)}
                        />
                        Quiero registrarme como profesional
                      </label>
                  )}
              />

              <formData.Subscribe
                  selector={(state) => state.values.esProfesional}
                  children={(esProfesional) =>
                      esProfesional ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: 16, marginTop: 12 }}>
                            <formData.AppField
                                name="profesion"
                                children={(profField) => (
                                    <profField.TextField label="Profesión" />
                                )}
                            />
                            <formData.AppField
                                name="sector"
                                children={(sectorField) => (
                                    <div>
                                      <label style={{ fontSize: 14, fontWeight: 500 }}>Sector</label>
                                      <select
                                          value={sectorField.state.value as string ?? ""}
                                          onChange={(e) => sectorField.handleChange(e.target.value)}
                                          style={{
                                            display: "block",
                                            marginTop: 4,
                                            padding: "8px 12px",
                                            borderRadius: 6,
                                            border: "1px solid #d1d5db",
                                            fontSize: 14,
                                            width: "100%",
                                          }}
                                      >
                                        <option value="">Seleccioná un sector</option>
                                        {SECTORES.map((s) => (
                                            <option key={s.value} value={s.value}>
                                              {s.label}
                                            </option>
                                        ))}
                                      </select>
                                      {sectorField.state.meta.errors.length > 0 && (
                                          <ul style={{ color: "black", fontSize: 13, marginTop: 4, paddingLeft: 16 }}>
                                            {sectorField.state.meta.errors.map((error, i) => (
                                                <li key={i}>{error?.message ?? String(error)}</li>
                                            ))}
                                          </ul>
                                      )}
                                    </div>
                                )}
                            />
                            <formData.AppField
                                name="ubicacion"
                                children={(field) => <field.TextField label="Ubicación" />}
                            />
                          </div>
                      ) : null
                  }
              />
            </formData.FormContainer>
          </formData.AppForm>
        </div>
      </div>
    </CommonLayout>
  );
};
