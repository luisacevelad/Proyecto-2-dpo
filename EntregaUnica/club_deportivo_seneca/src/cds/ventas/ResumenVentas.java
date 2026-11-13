package cds.ventas;

/**
 * Totales de las ventas confirmadas de un rubro en un periodo.
 * Lo calcula Club.resumenVentas y lo muestra el reporte del administrador.
 */
public class ResumenVentas {
	private final Rubro rubro;
	private final int cantidad;
	private final double total;
	private final double impuestos;
	private final double propinas;

	public ResumenVentas(Rubro rubro, int cantidad, double total, double impuestos, double propinas) {
		this.rubro = rubro;
		this.cantidad = cantidad;
		this.total = total;
		this.impuestos = impuestos;
		this.propinas = propinas;
	}

	public Rubro getRubro() {
		return rubro;
	}

	/** Numero de ventas (o de mensualidades pagadas, para el rubro MENSUALIDAD). */
	public int getCantidad() {
		return cantidad;
	}

	public double getTotal() {
		return total;
	}

	/** IVA en la tienda, impoconsumo en la cafeteria; 0 en mensualidades (exentas). */
	public double getImpuestos() {
		return impuestos;
	}

	/** Solo aplica a la cafeteria. */
	public double getPropinas() {
		return propinas;
	}

	public boolean estaVacio() {
		return cantidad == 0;
	}
}
