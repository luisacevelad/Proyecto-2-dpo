package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.ReglaNegocioException;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.partidos.Suspension;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.RestriccionMedica;
import cds.usuarios.Socio;

@DisplayName("HU-ENT-02 Verificar el minimo de jugadores habilitados")
class HuEnt02MinimoHabilitadosTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

	private Socio[] llenar(Equipo equipo, int cantidad) throws ReglaNegocioException {
		Socio[] socios = new Socio[cantidad];
		for (int i = 0; i < cantidad; i++) {
			socios[i] = new Socio("s" + i, "1", "Socio" + i, "CC" + i, HOY.minusYears(20));
			equipo.agregarJugador(socios[i], "Campo", i + 1, HOY);
		}
		return socios;
	}

	@Nested
	@DisplayName("CA-1 futbol con al menos siete habilitados")
	class FutbolCompleto {

		@Test
		@DisplayName("confirma que el partido puede disputarse")
		void puedeDisputarse() throws ReglaNegocioException {
			Equipo equipo = new Equipo("Seneca FC", DeporteConjunto.FUTBOL, CategoriaEdad.MAYORES, 20);
			llenar(equipo, 7);

			assertTrue(equipo.puedeDisputarPartido());
			assertTrue(equipo.jugadoresNoDisponibles().isEmpty());
		}
	}

	@Nested
	@DisplayName("CA-2 baloncesto con cuatro habilitados")
	class BaloncestoIncompleto {

		@Test
		@DisplayName("indica que no puede jugarse y lista los no disponibles con su motivo")
		void noPuedeJugarse() throws ReglaNegocioException {
			Equipo equipo = new Equipo("Seneca BC", DeporteConjunto.BALONCESTO, CategoriaEdad.MAYORES, 12);
			Socio[] socios = llenar(equipo, 6);
			equipo.agregarSuspension(new Suspension(socios[0], "tarjeta roja", null));
			Fisioterapeuta fisio = new Fisioterapeuta("fisio", "1", "Fisio", "D1");
			socios[1].agregarRestriccion(new RestriccionMedica(fisio, "esguince de tobillo", HOY));

			assertFalse(equipo.puedeDisputarPartido());
			Map<Socio, String> noDisponibles = equipo.jugadoresNoDisponibles();
			assertEquals(2, noDisponibles.size());
			assertTrue(noDisponibles.get(socios[0]).contains("suspendido"), noDisponibles.get(socios[0]));
			assertTrue(noDisponibles.get(socios[1]).contains("esguince de tobillo"), noDisponibles.get(socios[1]));
		}
	}

	@Nested
	@DisplayName("CA-3 jugador con sancion vigente")
	class SancionVigente {

		@Test
		@DisplayName("no se incluye en el conteo de habilitados")
		void noSeCuenta() throws ReglaNegocioException {
			Equipo equipo = new Equipo("Seneca FC", DeporteConjunto.FUTBOL, CategoriaEdad.MAYORES, 20);
			Socio[] socios = llenar(equipo, 7);
			equipo.agregarSuspension(new Suspension(socios[3], "tarjeta roja", null));

			assertEquals(6, equipo.contarHabilitados());
			assertFalse(equipo.puedeDisputarPartido());
		}
	}
}
