import { z } from "zod";

export const UserRoleSchema = z.enum(["CLIENTE", "PROFESIONAL", "SUPER_ADMIN"]);

const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{8,}$/;

export const SignupRequestSchema = z.object({
  email: z.email("Ingresá un email válido"),
  password: z
    .string()
    .min(8, "La contraseña debe tener al menos 8 caracteres")
    .regex(
      passwordRegex,
      "La contraseña debe tener mayúsculas, minúsculas, un número y un símbolo"
    ),
  nombre: z.string().min(1, "El nombre es requerido"),
  apellido: z.string().min(1, "El apellido es requerido"),
  fechaNacimiento: z
    .string()
    .min(1, "La fecha de nacimiento es requerida")
    .refine((val) => new Date(val) < new Date(), "La fecha de nacimiento no puede ser en el futuro"),
  esProfesional: z.boolean(),
  profesion: z.string().nullable(),
  sector: z.string().nullable(),
  ubicacion: z.string().nullable(),
}).superRefine((data, ctx) => {
  if (data.esProfesional) {
    if (!data.profesion || data.profesion.trim() === "") {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "La profesión es requerida, escriba su profesión",
        path: ["profesion"],
      });
    }
    if (!data.sector || data.sector === "") {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "El sector es requerido",
        path: ["sector"],
      });
    }
    if (!data.ubicacion || data.ubicacion.trim() === "") {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "La ubicación es requerida",
        path: ["ubicacion"],
      });
    }
  }
});

export type SignupRequest = z.infer<typeof SignupRequestSchema>;

export const LoginRequestSchema = z.object({
  email: z.email("Ingresá un email válido"),
  password: z.string().min(1, "La contraseña es requerida"),
});

export type LoginRequest = z.infer<typeof LoginRequestSchema>;

export const AuthResponseSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  roles: z.array(UserRoleSchema).min(1),
  verified: z.boolean(),
});

export type AuthResponse = z.infer<typeof AuthResponseSchema>;
