package cds.catalogo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Empleado;

public class Sugerencia {

	private final Empleado autor;
	private final String descripcion;
	private final LocalDate fecha;
	private final TipoSugerencia tipo;
	private final double precio;
	private List<Alergeno> alergenos;
	private EstadoSugerencia estado;

	public Sugerencia(Empleado autor, String descripcion, LocalDate fecha, TipoSugerencia tipo, double precio) {
		this.autor = autor;
		this.descripcion = descripcion;
		this.fecha = fecha;
		this.tipo = tipo;
		this.precio = precio;
		alergenos = null;
		estado = EstadoSugerencia.PENDIENTE;
	}

	public void declararAlergenos(List<Alergeno> declarados) {
		alergenos = new ArrayList<Alergeno>(declarados);
	}

	public boolean tieneAlergenosDeclarados() {
		return alergenos != null;
	}

	public void aprobar() throws ReglaNegocioException {
		if (estado != EstadoSugerencia.PENDIENTE) {
			throw new ReglaNegocioException("Solo se puede aprobar una sugerencia pendiente (estado actual: " + estado + ")");
		}
		if (tipo == TipoSugerencia.SNACK && !tieneAlergenosDeclarados()) {
			throw new ReglaNegocioException("El snack " + descripcion
					+ " no tiene alergenos declarados; complete la informacion antes de aprobarlo");
		}
		estado = EstadoSugerencia.APROBADA;
	}

	public void rechazar() throws ReglaNegocioException {
		if (estado != EstadoSugerencia.PENDIENTE) {
			throw new ReglaNegocioException("Solo se puede rechazar una sugerencia pendiente (estado actual: " + estado + ")");
		}
		estado = EstadoSugerencia.RECHAZADA;
	}

	public Empleado getAutor() {
		return autor;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public TipoSugerencia getTipo() {
		return tipo;
	}

	public double getPrecio() {
		return precio;
	}

	public List<Alergeno> getAlergenos() {
		if (alergenos == null) {
			return new ArrayList<Alergeno>();
		}
		return new ArrayList<Alergeno>(alergenos);
	}

	public EstadoSugerencia getEstado() {
		return estado;
	}
}
