package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.ReglaNegocioException;
import cds.catalogo.Articulo;
import cds.catalogo.Inventario;
import cds.catalogo.Ubicacion;
import cds.ventas.TiendaCafeteria;

@DisplayName("HU-ADM-1 Trasladar un producto entre tiendas del club")
class HuAdm01TrasladoProductoTest {

	private TiendaCafeteria tienda;
	private Ubicacion origen;
	private Ubicacion destino;
	private Articulo balon;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		tienda = new TiendaCafeteria();
		origen = new Ubicacion("Tienda principal");
		destino = new Ubicacion("Tienda canchas");
		balon = new Articulo("Balon de futbol", "Implementos", 80000);
		tienda.registrarUbicacion(origen);
		tienda.registrarUbicacion(destino);
		tienda.registrarArticulo(balon);
	}

	@Nested
	@DisplayName("CA-1 producto en origen y no en destino")
	class TrasladoExitoso {

		@Test
		@DisplayName("deja de venderse en origen y empieza a venderse en destino")
		void trasladaTodoElInventario() throws ReglaNegocioException {
			tienda.registrarInventario(new Inventario(balon, origen, 10));

			tienda.trasladar(balon, origen, destino);

			assertNull(tienda.buscarInventario(balon, origen));
			Inventario enDestino = tienda.buscarInventario(balon, destino);
			assertNotNull(enDestino);
			assertEquals(10, enDestino.getCantidad());
		}
	}

	@Nested
	@DisplayName("cA-2 producto no esta en origen")
	class ProductoAusenteEnOrigen {

		@Test
		@DisplayName("rechaza el traslado y no cambia los inventarios")
		void rechazaTraslado() {
			assertThrows(ReglaNegocioException.class, () -> tienda.trasladar(balon, origen, destino));

			assertNull(tienda.buscarInventario(balon, origen));
			assertNull(tienda.buscarInventario(balon, destino));
		}
	}

	@Nested
	@DisplayName("CA-3 producto ya esta en destino")
	class ProductoYaEnDestino {

		@Test
		@DisplayName("rechaza el traslado y no cambia los inventarios")
		void rechazaTraslado() throws ReglaNegocioException {
			tienda.registrarInventario(new Inventario(balon, origen, 10));
			tienda.registrarInventario(new Inventario(balon, destino, 3));

			assertThrows(ReglaNegocioException.class, () -> tienda.trasladar(balon, origen, destino));

			assertEquals(10, tienda.buscarInventario(balon, origen).getCantidad());
			assertEquals(3, tienda.buscarInventario(balon, destino).getCantidad());
		}
	}
}
