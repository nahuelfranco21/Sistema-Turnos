import { describe, expect, it } from "vitest";

import { AuthResponseSchema } from "./Login";

describe("AuthResponseSchema", () => {
  const validResponse = {
    accessToken: "token-abc",
    refreshToken: "refresh-abc",
    roles: ["CLIENTE"],
    verified: false,
  };

  it("parsea una respuesta válida de login", () => {
    const result = AuthResponseSchema.parse(validResponse);
    expect(result.accessToken).toBe("token-abc");
    expect(result.refreshToken).toBe("refresh-abc");
    expect(result.roles).toContain("CLIENTE");
  });

  it("falla si falta el campo roles", () => {
    expect(() =>
      AuthResponseSchema.parse({ accessToken: "token-abc", refreshToken: "refresh-abc" }),
    ).toThrow();
  });

  it("falla si roles es un array vacío", () => {
    expect(() => AuthResponseSchema.parse({ ...validResponse, roles: [] })).toThrow();
  });

  it("falla si el rol no es válido", () => {
    expect(() => AuthResponseSchema.parse({ ...validResponse, roles: ["ADMIN"] })).toThrow();
  });

  it("parsea correctamente el rol SUPER_ADMIN", () => {
    const result = AuthResponseSchema.parse({ ...validResponse, roles: ["SUPER_ADMIN"] });
    expect(result.roles).toContain("SUPER_ADMIN");
  });
});
