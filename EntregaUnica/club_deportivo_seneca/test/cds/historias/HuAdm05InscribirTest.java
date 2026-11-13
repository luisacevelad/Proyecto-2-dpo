package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.competencias.Liga;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.torneos.EquipoTorneo;
import cds.torneos.EspecialidadConjunto;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.InscripcionTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Estudiante;
import cds.usuarios.Socio;

@DisplayName("HU-ADM-05 Inscribir equipo o externo")
class HuAdm05InscribirTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final LocalDate LIMITE = INICIO.minusDays(3);

	private Club club;
	private Liga liga;
	private int contador;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		liga = new Liga("Liga Sub-15", "Club", 2026, DeporteConjunto.FUTBOL, CategoriaEdad.SUB_15, 7, 3, 1);
		club.registrarCompetencia(liga);
		contador = 0;
	}

	private Equipo equipo(String nombre, CategoriaEdad categoria, int edad, int puntosLiga) throws ReglaNegocioException {
		Equipo e = new Equipo(nombre, DeporteConjunto.FUTBOL, categoria, 20);
		for (int i = 1; i <= 7; i++) {
			contador++;
			Socio s = new Socio("j" + contador, "1", "Jugador" + contador, "TI" + contador, HOY.minusYears(edad));
			e.agregarJugador(s, "Campo", i, HOY);
		}
		club.registrarEquipo(e);
		liga.restaurarPuntos(e, puntosLiga);
		return e;
	}

	private Torneo copaFutbol(int cupos) throws ReglaNegocioException {
		Torneo t = new TorneoRecreativo("Copa Sub-15", new EspecialidadConjunto(DeporteConjunto.FUTBOL, CategoriaEdad.SUB_15),
				cupos, INICIO, LIMITE, 15);
		club.registrarTorneo(t);
		return t;
	}

	@Nested
	@DisplayName("CA-1 equipo en el top de su liga")
	class EquipoTop {

		@Test
		@DisplayName("ocupa un cupo preferencial")
		void ocupaPreferencial() throws ReglaNegocioException {
			Equipo lideres = equipo("Lideres", CategoriaEdad.SUB_15, 15, 9);
			equipo("Coleros", CategoriaEdad.SUB_15, 15, 3);
			Torneo copa = copaFutbol(5);

			InscripcionTorneo i = club.inscribirEnTorneo(copa, new EquipoTorneo(lideres), HOY);

			assertTrue(i.isPreferencial());
			assertEquals(0, copa.cuposPreferencialesDisponibles(HOY));
			assertEquals(4, copa.cuposRegularesDisponibles(HOY));
		}
	}

	@Nested
	@DisplayName("CA-2 equipo de otra categoria o torneo lleno")
	class EquipoRechazado {

		@Test
		@DisplayName("rechaza un equipo de otra categoria")
		void otraCategoria() throws ReglaNegocioException {
			Equipo sub17 = equipo("Sub-17", CategoriaEdad.SUB_17, 16, 0);
			Torneo copa = copaFutbol(5);

			assertThrows(ReglaNegocioException.class, () -> club.inscribirEnTorneo(copa, new EquipoTorneo(sub17), HOY));
			assertTrue(copa.getInscripciones().isEmpty());
		}

		@Test
		@DisplayName("rechaza un equipo cuando el torneo esta lleno")
		void torneoLleno() throws ReglaNegocioException {
			Equipo a = equipo("A", CategoriaEdad.SUB_15, 15, 9);
			Equipo b = equipo("B", CategoriaEdad.SUB_15, 15, 6);
			Equipo c = equipo("C", CategoriaEdad.SUB_15, 15, 3);
			Torneo copa = copaFutbol(2);
			club.inscribirEnTorneo(copa, new EquipoTorneo(a), HOY);
			club.inscribirEnTorneo(copa, new EquipoTorneo(b), HOY);

			assertThrows(ReglaNegocioException.class, () -> club.inscribirEnTorneo(copa, new EquipoTorneo(c), HOY));
			assertEquals(2, copa.getInscripciones().size());
		}
	}

	@Nested
	@DisplayName("CA-3 estudiante en torneo individual")
	class EstudianteEnTorneo {

		@Test
		@DisplayName("queda registrado con la tarifa a cobrar")
		void registraConTarifa() throws ReglaNegocioException {
			TorneoCompetitivo abierto = new TorneoCompetitivo("Abierto",
					new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false), 8, INICIO, LIMITE, 50000);
			club.registrarTorneo(abierto);
			Estudiante nico = new Estudiante("nico", "1", "Nico", "202412345");

			InscripcionTorneo i = club.inscribirEnTorneo(abierto, new JugadorTorneo(nico), HOY);

			assertEquals(50000, i.getMontoPagado());
			assertFalse(i.isPreferencial());
			assertEquals(50000, abierto.totalRecaudado());
		}
	}
}
