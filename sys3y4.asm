main: push bp             ; Guarda el BP anterior para establecer un nuevo marco de pila [3]
    mov bp,sp           ; Establece BP al inicio del marco de pila actual [3]

; --- Leer un string del usuario (SYS 3) ---
; SYS 3 (STRING READ) requiere:
; EDX: Dirección de memoria donde almacenar el string leído [4, 5].
; CX: Cantidad máxima de caracteres a leer (incluyendo el terminador '\0') [4, 5].

    SYS 0x7; clear screen

    MOV EDX, DS         ; Cargamos la base del Data Segment en EDX [6]
    ADD EDX, 0          ; Sumamos el offset 0 para apuntar al inicio del Data Segment.
                        ; Aquí almacenaremos el string.

    MOV CX, 256         ; Establecemos la cantidad máxima de caracteres a leer (el tamaño de nuestro buffer) [4, 5].
                        ; Esto evita desbordamientos si el usuario ingresa un string muy largo.

    SYS 0x3             ; Llamamos a la función del sistema 3 para leer un string [4, 5].
                        ; La entrada se guarda en la memoria apuntada por EDX y se termina con '\0'.
	sys 0x7
; --- Mostrar el string leído (SYS 4) ---
; SYS 4 (STRING WRITE) requiere:
; EDX: Dirección de memoria del string a imprimir [7, 8].
; La impresión continúa hasta encontrar el terminador '\0' [8].

    MOV EDX, DS         ; Cargamos la base del Data Segment nuevamente en EDX [6].
    ADD EDX, 0          ; Sumamos el offset 0 para apuntar al inicio del Data Segment,
                        ; que es donde se almacenó el string leído por SYS 3.

    SYS 0x4             ; Llamamos a la función del sistema 4 para imprimir el string [7, 8].

; --- Finalizar el programa ---
; Como estamos en la rutina principal (main) y queremos terminar la ejecución del proceso,
; usamos STOP en lugar de RET.

    STOP                ; Detiene la ejecución del
