package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;

@DisplayName("HU-ADM-04 Crear torneo")
class HuAdm04CrearTorneoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final LocalDate LIMITE = INICIO.minusDays(3);
	private static final EspecialidadIndividual TENIS = new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false);

	private Club club;

	@BeforeEach
	void setUp() {
		club = new Club();
	}

	@Nested
	@DisplayName("CA-1 cupos preferenciales del 20% redondeado hacia arriba")
	class CuposPreferenciales {

		@ParameterizedTest(name = "{0} cupos -> {1} preferenciales y {2} regulares")
		@CsvSource({ "10, 2, 8", "11, 3, 8", "8, 2, 6", "2, 1, 1" })
		@DisplayName("divide los cupos al crear el torneo")
		void divideCupos(int cupos, int preferenciales, int regulares) throws ReglaNegocioException {
			Torneo t = new TorneoCompetitivo("Abierto de tenis", TENIS, cupos, INICIO, LIMITE, 50000);

			club.registrarTorneo(t);

			assertEquals(preferenciales, t.getCuposPreferenciales());
			assertEquals(regulares, t.cuposRegularesDisponibles(HOY));
			assertEquals(EstadoTorneo.INSCRIPCIONES_ABIERTAS, t.getEstado());
			assertEquals(t, club.buscarTorneo("Abierto de tenis"));
		}
	}

	@Nested
	@DisplayName("CA-2 datos invalidos")
	class DatosInvalidos {

		@Test
		@DisplayName("rechaza un nombre repetido")
		void nombreRepetido() throws ReglaNegocioException {
			club.registrarTorneo(new TorneoRecreativo("Copa", TENIS, 8, INICIO, LIMITE, 15));

			assertThrows(ReglaNegocioException.class,
					() -> club.registrarTorneo(new TorneoRecreativo("Copa", TENIS, 4, INICIO, LIMITE, 10)));
			assertEquals(1, club.getTorneos().size());
		}

		@ParameterizedTest(name = "tarifa {0}")
		@ValueSource(doubles = { 0, -1000 })
		@DisplayName("rechaza un torneo competitivo sin tarifa")
		void competitivoSinTarifa(double tarifa) {
			assertThrows(ReglaNegocioException.class,
					() -> new TorneoCompetitivo("Copa", TENIS, 8, INICIO, LIMITE, tarifa));
		}

		@ParameterizedTest(name = "{0} cupos")
		@ValueSource(ints = { 0, 1 })
		@DisplayName("rechaza menos de 2 cupos")
		void pocosCupos(int cupos) {
			assertThrows(ReglaNegocioException.class, () -> new TorneoRecreativo("Copa", TENIS, cupos, INICIO, LIMITE, 15));
		}

		@Test
		@DisplayName("rechaza una fecha limite de preferenciales que no sea anterior al inicio")
		void limiteDespuesDelInicio() {
			assertThrows(ReglaNegocioException.class, () -> new TorneoRecreativo("Copa", TENIS, 8, INICIO, INICIO, 15));
		}
	}
}
