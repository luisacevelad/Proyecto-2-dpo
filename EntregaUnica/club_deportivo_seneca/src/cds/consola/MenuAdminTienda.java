package cds.consola;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.catalogo.Alergeno;
import cds.catalogo.Articulo;
import cds.catalogo.Bebida;
import cds.catalogo.EstadoSugerencia;
import cds.catalogo.Inventario;
import cds.catalogo.ProductoCafeteria;
import cds.catalogo.Snack;
import cds.catalogo.Sugerencia;
import cds.catalogo.TipoSugerencia;
import cds.catalogo.Ubicacion;
import cds.usuarios.Socio;
import cds.ventas.ResumenVentas;
import cds.ventas.Rubro;
import cds.ventas.TiendaCafeteria;

/** Administracion de la tienda, la cafeteria y los reportes (HU-ADM-01 y HU-ADM-03). */
public class MenuAdminTienda {
	private final ConsolaAdministrador consola;
	private final Entrada entrada;

	public MenuAdminTienda(ConsolaAdministrador consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		Menu catalogo = new Menu("Catalogo e inventario", entrada)
				.agregar("Ver inventario por tienda", this::verInventario)
				.agregar("Registrar articulo", this::registrarArticulo)
				.agregar("Registrar producto de cafeteria", this::registrarProducto)
				.agregar("Registrar tienda o ubicacion", this::registrarUbicacion)
				.agregar("Reabastecer inventario", this::reabastecer)
				.agregar("Trasladar un producto a otra tienda", this::trasladar);
		return new Menu("Tienda, cafeteria y reportes", entrada)
				.agregarSubmenu("Catalogo e inventario", catalogo)
				.agregar("Revisar sugerencias de empleados", this::revisarSugerencias)
				.agregar("Generar cobro de mensualidad", this::generarMensualidad)
				.agregar("Reporte de ingresos por rubro", this::reporteIngresos);
	}

	private TiendaCafeteria tienda() {
		return consola.club.getTienda();
	}

	private Ubicacion elegirUbicacion(String titulo) throws ReglaNegocioException {
		return entrada.elegir(titulo, tienda().getUbicaciones(), Ubicacion::getNombre);
	}

	private void verInventario() {
		for (Ubicacion u : tienda().getUbicaciones()) {
			System.out.println(u.getNombre() + ":");
			List<Inventario> inventario = tienda().inventariosEn(u);
			if (inventario.isEmpty()) {
				System.out.println("   (sin productos)");
			}
			for (Inventario i : inventario) {
				System.out.println("   " + i.getArticulo().getNombre() + ": " + i.getCantidad() + " unidades");
			}
		}
	}

	private void registrarArticulo() throws ReglaNegocioException {
		String nombre = entrada.texto("Nombre del articulo");
		String categoria = entrada.texto("Categoria");
		double precio = entrada.decimal("Precio", 1, 100000000);
		tienda().registrarArticulo(new Articulo(nombre, categoria, precio));
		System.out.println("Articulo registrado. Use 'Reabastecer inventario' para ponerlo a la venta.");
	}

	private void registrarProducto() throws ReglaNegocioException {
		String nombre = entrada.texto("Nombre del producto");
		double precio = entrada.decimal("Precio", 1, 10000000);
		ProductoCafeteria producto;
		if (entrada.siNo("Es una bebida? (n = snack)")) {
			producto = new Bebida(nombre, precio, entrada.siNo("Es caliente?"));
		} else {
			producto = new Snack(nombre, precio);
		}
		for (Alergeno a : pedirAlergenos(entrada)) {
			producto.agregarAlergeno(a);
		}
		tienda().registrarProducto(producto);
		System.out.println("Producto registrado en la cafeteria.");
	}

	/** Pide los alergenos uno por uno (tambien lo usa la consola de empleado). */
	static List<Alergeno> pedirAlergenos(Entrada entrada) {
		List<Alergeno> alergenos = new ArrayList<Alergeno>();
		while (entrada.siNo(alergenos.isEmpty() ? "Tiene alergenos?" : "Agregar otro alergeno?")) {
			Alergeno a = entrada.elegirEnum("Alergeno", Alergeno.values());
			if (!alergenos.contains(a)) {
				alergenos.add(a);
			}
		}
		return alergenos;
	}

	private void registrarUbicacion() throws ReglaNegocioException {
		tienda().registrarUbicacion(new Ubicacion(entrada.texto("Nombre de la tienda o ubicacion")));
		System.out.println("Ubicacion registrada.");
	}

	private void reabastecer() throws ReglaNegocioException {
		Articulo articulo = entrada.elegir("Articulos", tienda().getArticulos(),
				a -> a.getNombre() + " - " + ConsolaBase.pesos(a.getPrecio()));
		Ubicacion ubicacion = elegirUbicacion("Tienda");
		int unidades = entrada.entero("Unidades a agregar", 1, 100000);
		Inventario inventario = tienda().buscarInventario(articulo, ubicacion);
		if (inventario == null) {
			tienda().registrarInventario(new Inventario(articulo, ubicacion, unidades));
		} else {
			inventario.agregar(unidades);
		}
		System.out.println("Inventario actualizado: " + tienda().buscarInventario(articulo, ubicacion).getCantidad()
				+ " unidades de " + articulo.getNombre() + " en " + ubicacion.getNombre());
	}

	/** HU-ADM-01: el producto deja de venderse en el origen y pasa al destino. */
	private void trasladar() throws ReglaNegocioException {
		Ubicacion origen = elegirUbicacion("Tienda de origen");
		Articulo articulo = entrada.elegir("Articulos", tienda().getArticulos(), Articulo::getNombre);
		Ubicacion destino = elegirUbicacion("Tienda de destino");
		tienda().trasladar(articulo, origen, destino);
		System.out.println(articulo.getNombre() + " ahora se vende en " + destino.getNombre() + " y ya no en " + origen.getNombre() + ".");
	}

	/** HU-ADM-03: aprobar (crea el producto) o rechazar sugerencias. */
	private void revisarSugerencias() throws ReglaNegocioException {
		List<Sugerencia> pendientes = new ArrayList<Sugerencia>();
		for (Sugerencia s : tienda().getSugerencias()) {
			if (s.getEstado() == EstadoSugerencia.PENDIENTE) {
				pendientes.add(s);
			}
		}
		Sugerencia s = entrada.elegir("Sugerencias pendientes", pendientes, MenuAdminTienda::describir);
		if (entrada.siNo("Aprobar la sugerencia? (n = rechazar)")) {
			tienda().aprobarSugerencia(s);
			System.out.println(s.getDescripcion() + " fue aprobada y ya esta en el catalogo.");
		} else {
			tienda().rechazarSugerencia(s);
			System.out.println("Sugerencia rechazada.");
		}
	}

	static String describir(Sugerencia s) {
		String texto = s.getTipo() + ": " + s.getDescripcion() + " - " + ConsolaBase.pesos(s.getPrecio())
				+ " (de " + s.getAutor().getNombre() + ")";
		if (s.getTipo() == TipoSugerencia.SNACK) {
			texto += s.tieneAlergenosDeclarados() ? " alergenos: " + s.getAlergenos() : " SIN alergenos declarados";
		}
		return texto;
	}

	private void generarMensualidad() throws ReglaNegocioException {
		Socio s = consola.elegirSocio(consola.socios());
		int anio = entrada.entero("Anio", 2020, 2100);
		int mes = entrada.entero("Mes", 1, 12);
		YearMonth periodo = YearMonth.of(anio, mes);
		double monto = entrada.decimal("Monto", 1, 100000000);
		consola.club.generarMensualidad(s, periodo, monto);
		System.out.println("Mensualidad de " + periodo + " generada para " + s.getNombre() + ".");
	}

	private void reporteIngresos() throws ReglaNegocioException {
		LocalDate desde = entrada.fecha("Desde");
		LocalDate hasta = entrada.fecha("Hasta");
		boolean hayDatos = false;
		for (Rubro rubro : new Rubro[] { Rubro.TIENDA, Rubro.CAFETERIA, Rubro.MENSUALIDAD }) {
			ResumenVentas r = consola.club.resumenVentas(rubro, desde, hasta);
			if (!r.estaVacio()) {
				hayDatos = true;
				System.out.println(rubro + ": " + r.getCantidad() + (rubro == Rubro.MENSUALIDAD ? " pagos (exentos)" : " ventas")
						+ ", total " + ConsolaBase.pesos(r.getTotal())
						+ (rubro == Rubro.MENSUALIDAD ? "" : ", impuestos " + ConsolaBase.pesos(r.getImpuestos()))
						+ (rubro == Rubro.CAFETERIA ? ", propinas " + ConsolaBase.pesos(r.getPropinas()) : ""));
			}
		}
		if (!hayDatos) {
			System.out.println("No hay datos disponibles para el periodo.");
		}
	}
}
