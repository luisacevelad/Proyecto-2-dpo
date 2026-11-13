package cds.requerimientos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.catalogo.Articulo;
import cds.catalogo.Snack;
import cds.usuarios.Socio;
import cds.ventas.Mensualidad;
import cds.ventas.ResumenVentas;
import cds.ventas.Rubro;
import cds.ventas.VentaCafeteria;
import cds.ventas.VentaTienda;

/** Requerimientos de mensualidades y del reporte de ingresos por rubro. */
@DisplayName("RF Mensualidades y reporte de ingresos")
class MensualidadesYReporteTest {
	private static final double DELTA = 0.001;
	private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
	private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

	private Club club;
	private Socio ana;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		ana = new Socio("ana", "1", "Ana", "D1", LocalDate.of(2000, 1, 1));
		club.registrarUsuario(ana);
	}

	@Nested
	@DisplayName("Mensualidades")
	class Mensualidades {

		@Test
		@DisplayName("se genera el cobro y queda pendiente hasta que se paga")
		void generaYPaga() throws ReglaNegocioException {
			Mensualidad m = club.generarMensualidad(ana, YearMonth.of(2026, 11), 150000);
			assertEquals(1, club.getTienda().getMensualidades().size());
			assertTrue(!m.isPagada());
			m.pagar(LocalDate.of(2026, 11, 3));
			assertTrue(m.isPagada());
		}

		@Test
		@DisplayName("no se puede cobrar dos veces el mismo mes")
		void mesDuplicado() throws ReglaNegocioException {
			club.generarMensualidad(ana, YearMonth.of(2026, 11), 150000);
			assertThrows(ReglaNegocioException.class, () -> club.generarMensualidad(ana, YearMonth.of(2026, 11), 150000));
		}

		@Test
		@DisplayName("el monto debe ser positivo")
		void montoInvalido() {
			assertThrows(ReglaNegocioException.class, () -> club.generarMensualidad(ana, YearMonth.of(2026, 11), 0));
		}

		@Test
		@DisplayName("no se puede pagar dos veces")
		void pagoDoble() throws ReglaNegocioException {
			Mensualidad m = club.generarMensualidad(ana, YearMonth.of(2026, 11), 150000);
			m.pagar(LocalDate.of(2026, 11, 3));
			assertThrows(ReglaNegocioException.class, () -> m.pagar(LocalDate.of(2026, 11, 4)));
		}
	}

	@Nested
	@DisplayName("Reporte de ingresos por rubro")
	class Reporte {

		@BeforeEach
		void ventas() throws ReglaNegocioException {
			Articulo balon = new Articulo("Balon", "Implementos", 100000);
			VentaTienda vt = new VentaTienda(LocalDate.of(2026, 11, 5), ana);
			vt.agregarLinea(balon, 1);
			vt.confirmar();
			club.getTienda().registrarVenta(vt);

			VentaCafeteria vc = new VentaCafeteria(LocalDate.of(2026, 11, 6), ana);
			vc.agregarLinea(new Snack("Galleta", 10000), 1);
			vc.confirmar();
			club.getTienda().registrarVenta(vc);

			club.generarMensualidad(ana, YearMonth.of(2026, 11), 150000).pagar(LocalDate.of(2026, 11, 2));
		}

		@Test
		@DisplayName("separa impuestos y propinas de cada rubro")
		void resumenPorRubro() throws ReglaNegocioException {
			ResumenVentas tienda = club.resumenVentas(Rubro.TIENDA, NOV_1, NOV_30);
			assertEquals(1, tienda.getCantidad());
			assertEquals(119000, tienda.getTotal(), DELTA);
			assertEquals(19000, tienda.getImpuestos(), DELTA);

			ResumenVentas cafeteria = club.resumenVentas(Rubro.CAFETERIA, NOV_1, NOV_30);
			assertEquals(800, cafeteria.getImpuestos(), DELTA);
			assertEquals(1000, cafeteria.getPropinas(), DELTA);
			assertEquals(11800, cafeteria.getTotal(), DELTA);

			ResumenVentas mensualidades = club.resumenVentas(Rubro.MENSUALIDAD, NOV_1, NOV_30);
			assertEquals(1, mensualidades.getCantidad());
			assertEquals(150000, mensualidades.getTotal(), DELTA);
			assertEquals(0, mensualidades.getImpuestos(), DELTA);
		}

		@Test
		@DisplayName("un periodo sin movimientos queda vacio")
		void periodoSinDatos() throws ReglaNegocioException {
			assertTrue(club.resumenVentas(Rubro.TIENDA, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)).estaVacio());
		}

		@Test
		@DisplayName("rechaza un rango con la fecha final antes que la inicial")
		void rangoInvalido() {
			assertThrows(ReglaNegocioException.class, () -> club.resumenVentas(Rubro.TIENDA, NOV_30, NOV_1));
		}
	}
}
