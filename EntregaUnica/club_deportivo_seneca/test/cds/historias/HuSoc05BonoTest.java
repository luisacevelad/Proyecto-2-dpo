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

import cds.ReglaNegocioException;
import cds.catalogo.Articulo;
import cds.torneos.BonoDescuento;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;
import cds.ventas.VentaTienda;

@DisplayName("HU-SOC-05 Usar bono de descuento")
class HuSoc05BonoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

	private Socio lucho;
	private BonoDescuento bono;
	private VentaTienda venta;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		lucho = new Socio("lucho", "1", "Lucho", "CC-1", HOY.minusYears(25));
		bono = new BonoDescuento(15, "Copa de tenis");
		lucho.agregarBono(bono);
		venta = new VentaTienda(HOY, lucho);
		venta.agregarLinea(new Articulo("Raqueta", "Implementos", 40000), 1);
	}

	@Nested
	@DisplayName("CA-1 bono del 15% en una compra de 40.000")
	class AplicarBono {

		@Test
		@DisplayName("descuenta 6.000 y el bono queda usado")
		void aplicaDescuento() throws ReglaNegocioException {
			double totalSinBono = venta.total();

			venta.aplicarBono(bono);
			venta.confirmar();

			assertEquals(6000, venta.descuentoBono());
			assertEquals(totalSinBono - 6000, venta.total(), 0.001);
			assertTrue(bono.isUsado());
			assertTrue(lucho.bonosDisponibles().isEmpty());
		}
	}

	@Nested
	@DisplayName("CA-2 bono usado u otro descuento en la compra")
	class BonoRechazado {

		@Test
		@DisplayName("rechaza un bono ya usado")
		void bonoUsado() throws ReglaNegocioException {
			bono.usar();

			assertThrows(ReglaNegocioException.class, () -> venta.aplicarBono(bono));
			assertEquals(0, venta.descuentoBono());
		}

		@Test
		@DisplayName("rechaza el bono si la compra ya redime puntos de fidelidad")
		void conPuntos() throws ReglaNegocioException {
			lucho.acreditarPuntosFidelidad(100);
			venta.redimirPuntos(100);

			assertThrows(ReglaNegocioException.class, () -> venta.aplicarBono(bono));
			assertFalse(bono.isUsado());
		}

		@Test
		@DisplayName("rechaza el bono si la compra ya tiene codigo de empleado")
		void conCodigo() throws ReglaNegocioException {
			venta.aplicarCodigo(new Entrenador("profe", "1", "Profe", "D1"));

			assertThrows(ReglaNegocioException.class, () -> venta.aplicarBono(bono));
		}
	}
}
