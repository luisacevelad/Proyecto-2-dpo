package cds.requerimientos;

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
import cds.academias.Instalacion;
import cds.academias.TipoInstalacion;
import cds.catalogo.Articulo;
import cds.catalogo.Bebida;
import cds.catalogo.Inventario;
import cds.catalogo.Snack;
import cds.catalogo.Ubicacion;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;
import cds.ventas.TiendaCafeteria;
import cds.ventas.VentaCafeteria;
import cds.ventas.VentaTienda;

/** Requerimientos de ventas en la tienda y la cafeteria (Proyecto 1). */
@DisplayName("RF Ventas: tienda y cafeteria")
class VentasTiendaCafeteriaTest {
	private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
	private static final double DELTA = 0.001;

	private TiendaCafeteria tienda;
	private Ubicacion local;
	private Articulo camiseta;
	private Socio ana;
	private Entrenador leo;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		tienda = new TiendaCafeteria();
		local = new Ubicacion("Tienda");
		camiseta = new Articulo("Camiseta", "Ropa", 50000);
		tienda.registrarUbicacion(local);
		tienda.registrarArticulo(camiseta);
		tienda.registrarInventario(new Inventario(camiseta, local, 5));
		ana = new Socio("ana", "1", "Ana", "D1", LocalDate.of(2000, 1, 1));
		leo = new Entrenador("leo", "1", "Leo", "LEO-CDS");
	}

	private VentaTienda ventaDeDosCamisetas() throws ReglaNegocioException {
		VentaTienda v = new VentaTienda(HOY, ana);
		v.agregarLinea(camiseta, 2);
		return v;
	}

	@Nested
	@DisplayName("Tienda: precios, descuentos e IVA")
	class Tienda {

		@Test
		@DisplayName("sin codigo cobra subtotal mas IVA del 19%")
		void sinCodigo() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			assertEquals(100000, v.subtotal(), DELTA);
			assertEquals(19000, v.impuesto(), DELTA);
			assertEquals(119000, v.total(), DELTA);
		}

		@Test
		@DisplayName("el codigo de otro empleado da 8% de descuento antes del IVA")
		void codigoCompartido() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			v.aplicarCodigo(leo);
			assertEquals(8000, v.descuento(), DELTA);
			assertEquals(92000 * 1.19, v.total(), DELTA);
		}

		@Test
		@DisplayName("el empleado que usa su propio codigo recibe 15%")
		void codigoPropio() throws ReglaNegocioException {
			VentaTienda v = new VentaTienda(HOY, leo);
			v.agregarLinea(camiseta, 2);
			v.aplicarCodigo(leo);
			assertEquals(15000, v.descuento(), DELTA);
		}

		@Test
		@DisplayName("no se puede aplicar un segundo codigo")
		void unSoloCodigo() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			v.aplicarCodigo(leo);
			assertThrows(ReglaNegocioException.class, () -> v.aplicarCodigo(leo));
		}

		@Test
		@DisplayName("descuenta el inventario y rechaza si no alcanzan las unidades")
		void inventario() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			v.descontarInventario(tienda.inventariosEn(local));
			assertEquals(3, tienda.buscarInventario(camiseta, local).getCantidad());

			VentaTienda grande = new VentaTienda(HOY, ana);
			grande.agregarLinea(camiseta, 4);
			assertThrows(ReglaNegocioException.class, () -> grande.descontarInventario(tienda.inventariosEn(local)));
		}

		@Test
		@DisplayName("un producto de cafeteria no se vende en la tienda")
		void productoEquivocado() {
			VentaTienda v = new VentaTienda(HOY, ana);
			assertThrows(ReglaNegocioException.class, () -> v.agregarLinea(new Snack("Galleta", 2000), 1));
		}
	}

	@Nested
	@DisplayName("Puntos de fidelidad")
	class Puntos {

		@Test
		@DisplayName("al registrar la venta se acredita el 2% del total")
		void acreditaPuntos() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			v.confirmar();
			tienda.registrarVenta(v);
			assertEquals(2380, ana.getPuntosFidelidad());
		}

		@Test
		@DisplayName("redimir puntos baja el total y los descuenta del saldo")
		void redimePuntos() throws ReglaNegocioException {
			ana.acreditarPuntosFidelidad(1000);
			VentaTienda v = ventaDeDosCamisetas();
			v.redimirPuntos(1000);
			assertEquals(118000, v.total(), DELTA);
			v.confirmar();
			tienda.registrarVenta(v);
			assertEquals(1000 - 1000 + (int) Math.floor(118000 * 0.02), ana.getPuntosFidelidad());
		}

		@Test
		@DisplayName("no se pueden redimir mas puntos de los que se tienen")
		void puntosInsuficientes() throws ReglaNegocioException {
			ana.acreditarPuntosFidelidad(10);
			VentaTienda v = ventaDeDosCamisetas();
			assertThrows(ReglaNegocioException.class, () -> v.redimirPuntos(11));
		}

		@Test
		@DisplayName("una venta sin confirmar no se registra")
		void sinConfirmar() throws ReglaNegocioException {
			VentaTienda v = ventaDeDosCamisetas();
			assertThrows(ReglaNegocioException.class, () -> tienda.registrarVenta(v));
			assertTrue(tienda.getVentas().isEmpty());
		}
	}

	@Nested
	@DisplayName("Cafeteria: impoconsumo, propina y bebidas calientes")
	class Cafeteria {
		private Bebida latte;
		private Snack galleta;

		@BeforeEach
		void productos() {
			latte = new Bebida("Latte", 10000, true);
			galleta = new Snack("Galleta", 10000);
		}

		@Test
		@DisplayName("cobra impoconsumo del 8% y propina sugerida del 10%")
		void impoconsumoYPropina() throws ReglaNegocioException {
			VentaCafeteria v = new VentaCafeteria(HOY, ana);
			v.agregarLinea(galleta, 1);
			assertEquals(800, v.impuesto(), DELTA);
			assertEquals(1000, v.cargosAdicionales(), DELTA);
			assertEquals(11800, v.total(), DELTA);
		}

		@Test
		@DisplayName("la propina se puede cambiar, pero no puede ser negativa")
		void propinaModificable() throws ReglaNegocioException {
			VentaCafeteria v = new VentaCafeteria(HOY, ana);
			v.agregarLinea(galleta, 1);
			v.setPorcentajePropina(0);
			assertEquals(10800, v.total(), DELTA);
			assertThrows(ReglaNegocioException.class, () -> v.setPorcentajePropina(-0.1));
		}

		@Test
		@DisplayName("una bebida caliente necesita saber en que instalacion se entrega")
		void bebidaCalienteSinInstalacion() throws ReglaNegocioException {
			VentaCafeteria v = new VentaCafeteria(HOY, ana);
			v.agregarLinea(latte, 1);
			v.confirmar();
			assertThrows(ReglaNegocioException.class, () -> tienda.registrarVenta(v));
		}

		@Test
		@DisplayName("RN-20: no se despacha bebida caliente en mesas techadas ni en padel")
		void instalacionRestringida() throws ReglaNegocioException {
			VentaCafeteria v = new VentaCafeteria(HOY, ana);
			v.agregarLinea(latte, 1);
			Instalacion mesa = new Instalacion("Mesa techada", TipoInstalacion.MESA_TENIS_MESA, true);
			Instalacion padel = new Instalacion("Padel", TipoInstalacion.CANCHA_PADEL, true);
			assertThrows(ReglaNegocioException.class, () -> v.validarDespacho(mesa));
			assertThrows(ReglaNegocioException.class, () -> v.validarDespacho(padel));
		}

		@Test
		@DisplayName("al entregarla en una cancha permitida, el socio queda con bebida caliente")
		void marcaBebidaCaliente() throws ReglaNegocioException {
			VentaCafeteria v = new VentaCafeteria(HOY, ana);
			v.agregarLinea(latte, 1);
			v.confirmar();
			tienda.registrarVenta(v, new Instalacion("Cancha 1", TipoInstalacion.CANCHA_TENIS, false));
			assertTrue(ana.tieneBebidaCaliente());
			ana.terminarBebidaCaliente();
			assertFalse(ana.tieneBebidaCaliente());
		}
	}
}
