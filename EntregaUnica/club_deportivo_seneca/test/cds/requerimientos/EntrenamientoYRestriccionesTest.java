package cds.requerimientos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.entrenamiento.Asistencia;
import cds.entrenamiento.EstadoAsistencia;
import cds.entrenamiento.SesionEntrenamiento;
import cds.usuarios.Entrenador;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.RestriccionMedica;
import cds.usuarios.Socio;

/** Requerimientos de sesiones de entrenamiento y restricciones medicas. */
@DisplayName("RF Entrenamientos y restricciones medicas")
class EntrenamientoYRestriccionesTest {
	private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

	private Club club;
	private Equipo equipo;
	private Entrenador leo;
	private Fisioterapeuta paula;
	private Socio ana;
	private Socio beto;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		leo = new Entrenador("leo", "1", "Leo", "LEO");
		paula = new Fisioterapeuta("paula", "1", "Paula", "PAU");
		ana = new Socio("ana", "1", "Ana", "D1", LocalDate.of(2000, 1, 1));
		beto = new Socio("beto", "1", "Beto", "D2", LocalDate.of(1999, 1, 1));
		equipo = new Equipo("Rojo", DeporteConjunto.BALONCESTO, CategoriaEdad.MAYORES, 10);
		club.registrarEquipo(equipo);
		club.inscribirJugador(equipo, ana, "Base", 1, HOY);
	}

	private SesionEntrenamiento nuevaSesion() {
		SesionEntrenamiento s = new SesionEntrenamiento(HOY, LocalTime.of(18, 0), 90, leo, null, equipo, null);
		club.registrarSesion(s);
		return s;
	}

	@Nested
	@DisplayName("Sesiones de entrenamiento")
	class Sesiones {

		@Test
		@DisplayName("convoca a un jugador del equipo, una sola vez")
		void convoca() throws ReglaNegocioException {
			SesionEntrenamiento s = nuevaSesion();
			s.convocar(ana);
			assertEquals(1, s.getConvocados().size());
			assertThrows(ReglaNegocioException.class, () -> s.convocar(ana));
		}

		@Test
		@DisplayName("no convoca a quien no es del equipo")
		void noEsDelEquipo() {
			SesionEntrenamiento s = nuevaSesion();
			assertThrows(ReglaNegocioException.class, () -> s.convocar(beto));
		}

		@Test
		@DisplayName("no convoca a un jugador con restriccion medica")
		void inhabilitado() {
			club.registrarRestriccion(paula, ana, "Esguince", HOY);
			SesionEntrenamiento s = nuevaSesion();
			assertThrows(ReglaNegocioException.class, () -> s.convocar(ana));
		}

		@Test
		@DisplayName("registra asistencia y calificacion entre 1 y 5")
		void asistenciaYNota() throws ReglaNegocioException {
			SesionEntrenamiento s = nuevaSesion();
			Asistencia a = s.convocar(ana);
			s.finalizar();
			a.registrarEstado(EstadoAsistencia.LLEGO_TARDE);
			a.calificar(4, "Buen trabajo");
			assertEquals(4, a.getEvaluacion().getNota());
			assertEquals(1, club.convocatorias(ana));
			assertEquals(1, club.asistencias(ana));
			assertThrows(ReglaNegocioException.class, () -> a.calificar(6, "Nota invalida"));
		}

		@Test
		@DisplayName("una falta cuenta como convocatoria pero no como asistencia")
		void falta() throws ReglaNegocioException {
			SesionEntrenamiento s = nuevaSesion();
			Asistencia a = s.convocar(ana);
			s.finalizar();
			a.registrarEstado(EstadoAsistencia.FALTO);
			assertEquals(1, club.convocatorias(ana));
			assertEquals(0, club.asistencias(ana));
		}
	}

	@Nested
	@DisplayName("Restricciones medicas")
	class Restricciones {

		@Test
		@DisplayName("una restriccion inhabilita y el alta vuelve a habilitar")
		void restriccionYAlta() throws ReglaNegocioException {
			club.registrarRestriccion(paula, ana, "Esguince", HOY);
			assertFalse(ana.estaHabilitado());
			RestriccionMedica r = ana.getRestricciones().get(0);
			r.darDeAlta(HOY.plusDays(10));
			assertTrue(ana.estaHabilitado());
		}

		@Test
		@DisplayName("el alta no puede ser antes del inicio ni repetirse")
		void altaInvalida() throws ReglaNegocioException {
			club.registrarRestriccion(paula, ana, "Esguince", HOY);
			RestriccionMedica r = ana.getRestricciones().get(0);
			assertThrows(ReglaNegocioException.class, () -> r.darDeAlta(HOY.minusDays(1)));
			r.darDeAlta(HOY);
			assertThrows(ReglaNegocioException.class, () -> r.darDeAlta(HOY.plusDays(1)));
		}
	}
}
