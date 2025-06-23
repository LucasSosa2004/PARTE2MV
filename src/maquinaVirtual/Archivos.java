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
        memoriaKiB *= 1024;
        
        if (!"VMI25".equals(identificador) || version != 1) {
            throw new IOException("VMI inválido: " + identificador + " v" + version);
        }
        Registros registros = MV.getRegistros();
        TablaDescripSegmentosV2 tabla = MV.getTabla();
        
        // registros
        for (int reg = 0; reg < 16; reg++) {
            int valor = 0;
            for (int b = 0; b < 4; b++) {
                int read = fis.read();
                if (read == -1) throw new IOException("Registro incompleto en VMI");
                valor = (valor << 8) | (read & 0xFF);
            }
            registros.addRegistro(reg, valor);
        }

        // tabla
        for (int i = 0; i < 8; i++) {
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

            if (limite > 0) {
                String nombre = registros.buscarBaseEnRegistros(i);
                if(i == 0 && !(nombre.equals("CS") || nombre.equals("KS"))) {
                	tabla.agregarSegmento("PS", base, limite);
                }
                else {
                	tabla.agregarSegmento(nombre, base, limite);
                }
            }
        } 

        tabla.mostrarTabla();
        registros.mostrarRegistros();
        int CS = tabla.getIndice("CS")<<16;
        MV.setCSOperaciones(CS);
        
        // memoria
        MemoriaBase memoria = MV.getMemoria();
        int offset = 0,readByte;
        while (offset < memoriaKiB && (readByte = fis.read()) != -1) {
            memoria.escribirByte(offset, (byte)readByte);
            offset++;
        }
    }


    public void guardarArchivoVMI(Registros registros, MemoriaBase memoria, TablaDescripSegmentosV2 tabla) throws IOException {
        if (archivoVMI == null) throw new IOException("No hay archivo VMI definido para guardar");

        try (FileOutputStream fos = new FileOutputStream(archivoVMI)) {
            // Header
            fos.write("VMI25".getBytes()); // 5 bytes
            fos.write(1); // versión

            // Tamaño de memoria
            int memoriaKiB = memoria.getMemoriaRaw().length;
            fos.write((memoriaKiB >> 8) & 0xFF); // byte alto
            fos.write(memoriaKiB & 0xFF);        // byte bajo

            // Registros
            for (int i = 0; i < 16; i++) {
                int valor = registros.getRegistro(i);
                fos.write((valor >> 24) & 0xFF);
                fos.write((valor >> 16) & 0xFF);
                fos.write((valor >> 8) & 0xFF);
                fos.write(valor & 0xFF);
            }

            // Tabla de segmentos (PS, CS, DS, ES, SS, KS)
            for(int i=0;i<8;i++) {
            	DescriptorSegmento segmento = tabla.getSegmento(i);
                short base = 0;
                short limite = 0;
                if(segmento != null) {
            		base = segmento.getBase();
            		limite = segmento.getTamanio();
                }
                System.out.println("VMI"+base +" "+ limite);
                fos.write((base >> 8) & 0xFF);
                fos.write(base & 0xFF);
                fos.write((limite >> 8) & 0xFF);
                fos.write(limite & 0xFF);
                
            }
            /*
            int j=0;
            for (int i = 0; i < 8; i++) {
            	String nombre = tabla.getNombreSegmentoVMI(i);
            	DescriptorSegmento segmento = tabla.getSegmento(j); // puede ser null
                short base = 0;
                short limite = 0;
                if(segmento != null && segmento.getNombre().equals(nombre)) {
            		base = segmento.getBase();
            		limite = segmento.getTamanio();
            		j++;
                }
                System.out.println("VMI"+base +" "+ limite);
                fos.write((base >> 8) & 0xFF);
                fos.write(base & 0xFF);
                fos.write((limite >> 8) & 0xFF);
                fos.write(limite & 0xFF);
            }*/

            byte[] mem = memoria.getMemoriaRaw();
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
