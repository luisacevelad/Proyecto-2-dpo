package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.ReglaNegocioException;
import cds.catalogo.Alergeno;
import cds.catalogo.EstadoSugerencia;
import cds.catalogo.ProductoCafeteria;
import cds.catalogo.Sugerencia;
import cds.catalogo.TipoSugerencia;
import cds.usuarios.Entrenador;
import cds.ventas.TiendaCafeteria;

@DisplayName("HU-ADM-03 Aprobar o rechazar sugerencias de nuevos articulos")
class HuAdm03SugerenciasTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

	private TiendaCafeteria tienda;
	private Sugerencia granola;

	@BeforeEach
	void setUp() {
		tienda = new TiendaCafeteria();
		Entrenador autor = new Entrenador("ent", "1", "Entrenador", "D1");
		granola = new Sugerencia(autor, "Barra de granola", HOY, TipoSugerencia.SNACK, 4500);
		tienda.registrarSugerencia(granola);
	}

	@Nested
	@DisplayName("CA-1 snack con alergenos declarados")
	class SnackCompleto {

		@Test
		@DisplayName("al aprobarlo se crea el producto disponible para la venta")
		void creaProducto() throws ReglaNegocioException {
			granola.declararAlergenos(List.of(Alergeno.GLUTEN, Alergeno.FRUTOS_SECOS));

			tienda.aprobarSugerencia(granola);

			assertEquals(EstadoSugerencia.APROBADA, granola.getEstado());
			ProductoCafeteria producto = tienda.buscarProducto("Barra de granola");
			assertNotNull(producto);
			assertEquals(4500, producto.getPrecio());
			assertTrue(producto.getAlergenos().contains(Alergeno.GLUTEN));
		}
	}

	@Nested
	@DisplayName("CA-2 snack sin alergenos declarados")
	class SnackIncompleto {

		@Test
		@DisplayName("impide aprobarlo hasta que la informacion este completa")
		void impideAprobar() {
			assertThrows(ReglaNegocioException.class, () -> tienda.aprobarSugerencia(granola));

			assertEquals(EstadoSugerencia.PENDIENTE, granola.getEstado());
			assertNull(tienda.buscarProducto("Barra de granola"));
		}
	}

	@Nested
	@DisplayName("CA-3 sugerencia rechazada")
	class SugerenciaRechazada {

		@Test
		@DisplayName("el empleado ve el rechazo y el producto no aparece en el catalogo")
		void muestraRechazo() throws ReglaNegocioException {
			tienda.rechazarSugerencia(granola);

			assertEquals(EstadoSugerencia.RECHAZADA, granola.getEstado());
			assertNull(tienda.buscarProducto("Barra de granola"));
		}
	}
}
