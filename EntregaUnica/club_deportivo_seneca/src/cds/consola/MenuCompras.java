package cds.consola;

import java.util.List;

import cds.ReglaNegocioException;
import cds.academias.Instalacion;
import cds.catalogo.Articulo;
import cds.catalogo.Bebida;
import cds.catalogo.Inventario;
import cds.catalogo.ProductoCafeteria;
import cds.catalogo.Ubicacion;
import cds.usuarios.Empleado;
import cds.usuarios.Usuario;
import cds.ventas.Bono;
import cds.ventas.LineaVenta;
import cds.ventas.TiendaCafeteria;
import cds.ventas.Venta;
import cds.ventas.VentaCafeteria;
import cds.ventas.VentaTienda;

/**
 * Compras en la tienda y la cafeteria. Lo usan las consolas de socio y de empleado,
 * porque ambos pueden comprar.
 */
public class MenuCompras {
	private final ConsolaBase consola;

	public MenuCompras(ConsolaBase consola) {
		this.consola = consola;
	}

	public Menu construir() {
		Menu menu = new Menu("Tienda y cafeteria", consola.entrada);
		menu.agregar("Comprar en la tienda", this::comprarTienda);
		menu.agregar("Comprar en la cafeteria", this::comprarCafeteria);
		menu.agregar("Ya termine mi bebida caliente", this::terminarBebida);
		menu.agregar("Ver mis puntos de fidelidad", this::verPuntos);
		menu.agregar("Ver mis bonos de torneos", this::verBonos);
		return menu;
	}

	private TiendaCafeteria tienda() {
		return consola.club.getTienda();
	}

	private void comprarTienda() throws ReglaNegocioException {
		Entrada entrada = consola.entrada;
		Usuario comprador = consola.usuario;
		Ubicacion punto = entrada.elegir("Punto de venta", tienda().getUbicaciones(), Ubicacion::getNombre);
		VentaTienda venta = new VentaTienda(consola.hoy(), comprador);
		do {
			Inventario inv = entrada.elegir("Articulos disponibles", tienda().inventariosEn(punto),
					i -> i.getArticulo().getNombre() + " - " + ConsolaBase.pesos(i.getArticulo().getPrecio())
							+ " (quedan " + i.getCantidad() + ")");
			int disponible = inv.getCantidad() - unidadesEnCarrito(venta, inv.getArticulo());
			if (disponible <= 0) {
				System.out.println("Ya tiene en el carrito todas las unidades disponibles de " + inv.getArticulo().getNombre());
			} else {
				int cantidad = entrada.entero("Cantidad", 1, disponible);
				venta.agregarLinea(inv.getArticulo(), cantidad);
			}
		} while (entrada.siNo("Agregar otro articulo?"));
		if (venta.getLineas().isEmpty()) {
			throw new ReglaNegocioException("La compra no tiene articulos");
		}
		if (!ofrecerBono(venta)) {
			String codigo = entrada.textoOpcional("Codigo de descuento de empleado");
			if (!codigo.isEmpty()) {
				venta.aplicarCodigo(buscarDuenoCodigo(codigo));
			}
			ofrecerPuntos(venta);
		}
		System.out.println("Subtotal: " + ConsolaBase.pesos(venta.subtotal()));
		System.out.println("Descuento: -" + ConsolaBase.pesos(venta.descuento()));
		mostrarBono(venta);
		System.out.println("IVA (19%): " + ConsolaBase.pesos(venta.impuesto()));
		System.out.println("Puntos redimidos: -" + venta.getPuntosRedimidos());
		System.out.println("TOTAL: " + ConsolaBase.pesos(venta.total()));
		if (!entrada.siNo("Confirmar compra?")) {
			System.out.println("Compra cancelada.");
			return;
		}
		venta.descontarInventario(tienda().inventariosEn(punto));
		venta.confirmar();
		tienda().registrarVenta(venta);
		System.out.println("Compra registrada. Puntos ganados: " + venta.puntosGenerados()
				+ ". Saldo de puntos: " + comprador.getPuntosFidelidad());
	}

	private int unidadesEnCarrito(Venta venta, Articulo articulo) {
		int total = 0;
		for (LineaVenta l : venta.getLineas()) {
			if (l.getProducto() == articulo) {
				total += l.getCantidad();
			}
		}
		return total;
	}

	private Empleado buscarDuenoCodigo(String codigo) throws ReglaNegocioException {
		Empleado dueno = consola.club.buscarEmpleadoPorCodigo(codigo);
		if (dueno == null) {
			throw new ReglaNegocioException("El codigo " + codigo + " no existe");
		}
		return dueno;
	}

	private void ofrecerPuntos(Venta venta) throws ReglaNegocioException {
		int puntos = consola.usuario.getPuntosFidelidad();
		if (puntos > 0 && consola.entrada.siNo("Tiene " + puntos + " puntos. Desea redimir puntos?")) {
			int maximo = (int) Math.min(puntos, Math.floor(venta.total()));
			if (maximo <= 0) {
				System.out.println("El total de la compra no permite redimir puntos.");
				return;
			}
			venta.redimirPuntos(consola.entrada.entero("Puntos a redimir", 1, maximo));
		}
	}

	private void comprarCafeteria() throws ReglaNegocioException {
		Entrada entrada = consola.entrada;
		Usuario comprador = consola.usuario;
		VentaCafeteria venta = new VentaCafeteria(consola.hoy(), comprador);
		do {
			ProductoCafeteria p = entrada.elegir("Menu de cafeteria", tienda().getProductos(), this::describir);
			venta.agregarLinea(p, entrada.entero("Cantidad", 1, 20));
		} while (entrada.siNo("Agregar otro producto?"));
		if (!venta.alergenos().isEmpty()) {
			System.out.println("Atencion, el pedido contiene: " + venta.alergenos());
		}
		if (entrada.siNo("Propina sugerida del 10%. Desea cambiarla?")) {
			venta.setPorcentajePropina(entrada.entero("Porcentaje de propina", 0, 100) / 100.0);
		}
		if (!ofrecerBono(venta)) {
			ofrecerPuntos(venta);
		}
		Instalacion dondeEsta = null;
		if (venta.tieneBebidaCaliente()) {
			System.out.println("El pedido tiene una bebida caliente: indique donde la va a recibir.");
			dondeEsta = consola.elegirInstalacion(consola.instalaciones());
		}
		System.out.println("Subtotal: " + ConsolaBase.pesos(venta.subtotal()));
		mostrarBono(venta);
		System.out.println("Impoconsumo (8%): " + ConsolaBase.pesos(venta.impuesto()));
		System.out.println("Propina: " + ConsolaBase.pesos(venta.cargosAdicionales()));
		System.out.println("Puntos redimidos: -" + venta.getPuntosRedimidos());
		System.out.println("TOTAL: " + ConsolaBase.pesos(venta.total()));
		if (!entrada.siNo("Confirmar pedido?")) {
			System.out.println("Pedido cancelado.");
			return;
		}
		if (dondeEsta != null) {
			venta.validarDespacho(dondeEsta);
		}
		venta.confirmar();
		if (dondeEsta != null) {
			tienda().registrarVenta(venta, dondeEsta);
		} else {
			tienda().registrarVenta(venta);
		}
		System.out.println("Pedido registrado. Saldo de puntos: " + comprador.getPuntosFidelidad());
	}

	private String describir(ProductoCafeteria p) {
		String texto = p.getNombre() + " - " + ConsolaBase.pesos(p.getPrecio());
		if (p instanceof Bebida && ((Bebida) p).isCaliente()) {
			texto += " (caliente)";
		}
		if (!p.getAlergenos().isEmpty()) {
			texto += " alergenos: " + p.getAlergenos();
		}
		return texto;
	}

	/**
	 * Ofrece usar un bono ganado en un torneo recreativo. El bono no se acumula con
	 * otros descuentos, asi que si se usa ya no se preguntan codigo ni puntos.
	 * @return true si se aplico un bono
	 */
	private boolean ofrecerBono(Venta venta) throws ReglaNegocioException {
		List<Bono> bonos = consola.club.bonosDisponibles(consola.usuario);
		if (bonos.isEmpty()) {
			return false;
		}
		if (!consola.entrada.siNo("Tiene " + bonos.size() + " bono(s) de torneo. Usar uno? (no se acumula con codigos ni puntos)")) {
			return false;
		}
		Bono bono = consola.entrada.elegir("Bonos", bonos, MenuCompras::describir);
		venta.aplicarBono(bono);
		return true;
	}

	private static String describir(Bono b) {
		return b.getPorcentaje() + "% de descuento (ganado en " + b.getOrigen() + ")";
	}

	private static void mostrarBono(Venta venta) {
		if (venta.getBono() != null) {
			System.out.println("Bono " + venta.getBono().getPorcentaje() + "%: -" + ConsolaBase.pesos(venta.descuentoBono()));
		}
	}

	private void verBonos() {
		List<Bono> bonos = consola.club.bonosDisponibles(consola.usuario);
		if (bonos.isEmpty()) {
			System.out.println("No tiene bonos disponibles. Se ganan quedando primero en un torneo recreativo.");
			return;
		}
		for (Bono b : bonos) {
			System.out.println("- " + describir(b));
		}
	}

	private void terminarBebida() {
		if (!consola.usuario.tieneBebidaCaliente()) {
			System.out.println("No tiene una bebida caliente registrada.");
			return;
		}
		consola.usuario.terminarBebidaCaliente();
		System.out.println("Listo, ya puede usar las canchas de padel y las mesas techadas.");
	}

	private void verPuntos() {
		System.out.println("Puntos de fidelidad: " + consola.usuario.getPuntosFidelidad());
	}
}
