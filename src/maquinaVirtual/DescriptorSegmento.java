package maquinaVirtual;

public class DescriptorSegmento {
	private String nombre;
	private short base;
	private short tamanio;
	
	public DescriptorSegmento(String nombre, short base, short tamanio) {
		this.nombre = nombre;
		this.base = base;
		this.tamanio = tamanio;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public short getBase() {
		return base;
	}

	public void setBase(short base) {
		this.base = base;
	}

	public short getTamanio() {
		return tamanio;
	}

	public void setTamanio(short tamanio) {
		this.tamanio = tamanio;
	}

	public int getLimite() {
		return (base + tamanio - 1);
	}

	@Override
	public String toString() {
		return "DescriptorSegmento [nombre=" + nombre + ", base=" + base + ", tamanio=" + tamanio + "]";
	}
}
