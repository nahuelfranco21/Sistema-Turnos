import { useEffect, useState } from "react";
import { BASE_API_URL } from "@/config/app-query-client";
import { useAuthenticatedFetch } from "@/services/TokenContext";
import { useDebounce } from "../../hooks/useDebounce";
import { UserPublicDTO } from "./types";

export function useUserSearch(role: "PROFESIONAL" | "CLIENTE") {
  const authedFetch = useAuthenticatedFetch();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UserPublicDTO[]>([]);
  const [loading, setLoading] = useState(false);

  const debouncedQuery = useDebounce(query, 300);

  useEffect(() => {
    if (debouncedQuery.trim().length < 2) {
      setResults([]);
      return;
    }
    let active = true;
    (async () => {
      setLoading(true);
      try {
        const res = await authedFetch(`${BASE_API_URL}/users?role=${role}&q=${encodeURIComponent(debouncedQuery)}`);
        if (active && res.ok) setResults(await res.json());
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, role]);

  return { query, setQuery, results, setResults, loading };
}
