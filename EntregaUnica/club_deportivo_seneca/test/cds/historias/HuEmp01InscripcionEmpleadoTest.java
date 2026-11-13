package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.torneos.Inscripcion;
import cds.torneos.JugadorClub;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Entrenador;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.Turno;

@DisplayName("HU-EMP-01 Inscribirse o salirse como empleado")
class HuEmp01InscripcionEmpleadoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final DayOfWeek DIA_TORNEO = INICIO.getDayOfWeek();

	private Club club;
	private TorneoCompetitivo abierto;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		abierto = new TorneoCompetitivo("Abierto", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO, 50000);
		club.registrarTorneo(abierto);
	}

	private void turno(cds.usuarios.Empleado e, DayOfWeek dia) {
		club.registrarTurno(new Turno(e, dia, LocalTime.of(6, 0), LocalTime.of(14, 0)));
	}

	@Nested
	@DisplayName("CA-1 empleado sin turno el dia del torneo")
	class SinTurno {

		@Test
		@DisplayName("queda registrado sin pagar")
		void registraSinPagar() throws ReglaNegocioException {
			Entrenador lucho = new Entrenador("lucho", "1", "Lucho", "D1");
			turno(lucho, DIA_TORNEO.plus(1));

			Inscripcion i = club.inscribirEnTorneo(abierto, List.of(new JugadorClub(lucho)), HOY);

			assertEquals(0, i.getTarifa());
			assertEquals(0, abierto.totalRecaudado());
			assertEquals(1, abierto.getInscripciones().size());
		}

		@Test
		@DisplayName("puede salirse antes del inicio")
		void puedeSalirse() throws ReglaNegocioException {
			Entrenador lucho = new Entrenador("lucho", "1", "Lucho", "D1");
			club.inscribirEnTorneo(abierto, List.of(new JugadorClub(lucho)), HOY);

			abierto.retirar(new JugadorClub(lucho));

			assertTrue(abierto.getInscripciones().isEmpty());
		}
	}

	@Nested
	@DisplayName("CA-2 empleado con turno ese dia")
	class ConTurno {

		@Test
		@DisplayName("rechaza la inscripcion")
		void rechaza() {
			Fisioterapeuta juancho = new Fisioterapeuta("juancho", "1", "Juancho", "D2");
			turno(juancho, DIA_TORNEO);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,() -> club.inscribirEnTorneo(abierto, List.of(new JugadorClub(juancho)), HOY));

			assertTrue(ex.getMessage().contains("turno"), ex.getMessage());
			assertTrue(abierto.getInscripciones().isEmpty());
		}
	}

	@Nested
	@DisplayName("CA-3 torneo de conjunto")
	class TorneoConjunto {

		@Test
		@DisplayName("rechaza la inscripcion de un empleado")
		void rechaza() throws ReglaNegocioException {
			Torneo copa = new TorneoRecreativo("Copa", DeporteConjunto.FUTBOL, CategoriaEdad.MAYORES, 4, INICIO);
			club.registrarTorneo(copa);
			Entrenador nico = new Entrenador("nico", "1", "Nico", "D3");

			assertThrows(ReglaNegocioException.class,
					() -> club.inscribirEnTorneo(copa, List.of(new JugadorClub(nico)), HOY));
			assertTrue(copa.getInscripciones().isEmpty());
		}
	}
}
