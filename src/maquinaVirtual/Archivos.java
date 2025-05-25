package maquinaVirtual;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Archivos {
	
    private final boolean existeVMI;
    private final String archivoVMI;
    private final String archivoVMX;

    public Archivos(String archivoVMI, String archivoVMX) {
        this.existeVMI = archivoVMI != null;
        this.archivoVMI = archivoVMI;
        this.archivoVMX = archivoVMX;
    }
    

    public static Archivos desdeArgs(String[] args) {
        String vmx = null;
        String vmi = null;

        for (String arg : args) {
            if (arg.endsWith(".vmx")) vmx = arg;
            else if (arg.endsWith(".vmi")) vmi = arg;
        }

        return new Archivos(vmi, vmx);
    }

    public boolean hayImagenParaCargar() {
        return existeVMI && archivoVMX == null;
    }

    public void cargarEnMV(MaquinaVirtual MV) throws IOException  {
        if (!hayImagenParaCargar()) return;
 
    	FileInputStream fis = new FileInputStream(archivoVMI);
    	
    	byte[] id = new byte[5];
        if (fis.read(id) != 5) 
            throw new IOException("Header VMI incompleto (ID)");
        String identificador = new String(id);
        
        int version = fis.read();
        if (version == -1) 
            throw new IOException("Header VMI incompleto (versión)");

        
        int memHi = fis.read();
        int memLo = fis.read();
        if (memLo == -1) 
            throw new IOException("Header VMI incompleto (tamaño)");
        int memoriaKiB = (memHi << 8) | (memLo & 0xFF);
        
        System.out.println("\nCargando VMI:");
        System.out.println("Identificador: " + identificador);
        System.out.println("Versión: " + version);
        System.out.println("Tamaño memoria: " + memoriaKiB + " KiB");

        
        if (!"VMI25".equals(identificador) || version != 1) {
            throw new IOException("VMI inválido: " + identificador + " v" + version);
        }
        Registros registros = MV.getRegistros();
        TablaDescripSegmentosV2 tabla = MV.getTabla();
        
        // Primero cargamos la tabla de segmentos
        System.out.println("\nCargando tabla de segmentos:");
        for (int i = 0; i < 6; i++) {
            // 2 bytes base
            int hiBase = fis.read();
            int loBase = fis.read();
            if (loBase == -1) throw new IOException("Descriptor base incompleto");
            short base = (short)((hiBase << 8) | (loBase & 0xFF));

            // 2 bytes límite
            int hiLim = fis.read();
            int loLim = fis.read();
            if (loLim == -1) throw new IOException("Descriptor límite incompleto");
            short limite = (short)((hiLim << 8) | (loLim & 0xFF));

            // Agregamos el segmento con el nombre que corresponde según el orden
            String nombre = tabla.getNombreSegmento(i);
            System.out.printf("Segmento %s: Base=0x%04X, Límite=0x%04X, Tamaño=%d bytes\n", 
                            nombre, base, limite, limite - base + 1);
            tabla.agregarSegmento(nombre, base, limite);
        }

        // Luego cargamos la memoria
        System.out.println("\nCargando memoria:");
        MemoriaBase memoria = MV.getMemoria();
        int offset = 0;
        int readByte;
        int bytesLeidos = 0;
        while (offset < memoriaKiB * 1024 && (readByte = fis.read()) != -1) {
            if (bytesLeidos % 16 == 0) {
                System.out.printf("\n0x%04X: ", offset);
            }
            System.out.printf("%02X ", readByte);
            memoria.escribirByte(offset, (byte)readByte);
            offset++;
            bytesLeidos++;
        }
        System.out.println("\n");

        // Finalmente cargamos los registros
        System.out.println("\nCargando registros:");
        for (int reg = 0; reg < 16; reg++) {
            int valor = 0;
            for (int b = 0; b < 4; b++) {
                int read = fis.read();
                if (read == -1) throw new IOException("Registro incompleto en VMI");
                valor = (valor << 8) | (read & 0xFF);
            }
            String nombreReg = registros.getNombreRegistro(reg);
            System.out.printf("Registro %s: 0x%08X\n", nombreReg, valor);
            registros.addRegistro(reg, valor);
        }
    }

    public void guardarArchivoVMI(Registros registros, MemoriaBase memoria, TablaDescripSegmentosV2 tabla) throws IOException {
        if (archivoVMI == null) throw new IOException("No hay archivo VMI definido para guardar");

        System.out.println("\nGuardando estado en VMI:");
        try (FileOutputStream fos = new FileOutputStream(archivoVMI)) {
            // Header
            System.out.println("Escribiendo header: VMI25 v1");
            fos.write("VMI25".getBytes()); // 5 bytes
            fos.write(1); // versión

            // Tamaño de memoria
            int memoriaKiB = memoria.getMemoriaRaw().length / 1024;
            System.out.println("Tamaño memoria: " + memoriaKiB + " KiB");
            fos.write((memoriaKiB >> 8) & 0xFF); // byte alto
            fos.write(memoriaKiB & 0xFF);        // byte bajo

            System.out.println("\nGuardando registros:");
            // Registros
            for (int i = 0; i < 16; i++) {
                int valor = registros.getRegistro(i);
                String nombreReg = registros.getNombreRegistro(i);
                System.out.printf("Registro %s: 0x%08X\n", nombreReg, valor);
                fos.write((valor >> 24) & 0xFF);
                fos.write((valor >> 16) & 0xFF);
                fos.write((valor >> 8) & 0xFF);
                fos.write(valor & 0xFF);
            }

            System.out.println("\nGuardando tabla de segmentos:");
            // Tabla de segmentos - guardamos en el orden en que están en la tabla
            for (int i = 0; i < 6; i++) {
                DescriptorSegmento segmento = tabla.getSegmento(i);
                short base = 0;
                short limite = 0;
                if (segmento != null) {
                    base = segmento.getBase();
                    limite = segmento.getLimite();
                }
                String nombre = tabla.getNombreSegmento(i);
                System.out.printf("Segmento %s: Base=0x%04X, Límite=0x%04X, Tamaño=%d bytes\n", 
                                nombre, base, limite, limite - base + 1);
                fos.write((base >> 8) & 0xFF);
                fos.write(base & 0xFF);
                fos.write((limite >> 8) & 0xFF);
                fos.write(limite & 0xFF);
            }

            System.out.println("\nGuardando memoria:");
            // Memoria: escribimos todo el bloque de memoriaRaw
            byte[] mem = memoria.getMemoriaRaw();
            for (int i = 0; i < mem.length; i++) {
                if (i % 16 == 0) {
                    System.out.printf("\n0x%04X: ", i);
                }
                System.out.printf("%02X ", mem[i] & 0xFF);
            }
            System.out.println("\n");
            fos.write(mem);
        }
    }


    public boolean tieneVMX() {
    	return archivoVMX != null;
    }
    public boolean tieneVMI() {
    	return archivoVMI != null;
    }
    public String getVMI() {
        return archivoVMI;
    }

    public String getVMX() {
        return archivoVMX;
    }
}
