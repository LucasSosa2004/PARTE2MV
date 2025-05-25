\\include "alloc25.asm"
\\include "listas25.asm"
\\STACK 1024

head        equ     4
main:       push   bp
            mov    bp, sp
            sub    sp, 4 ; head  (variable local)
            push   eax             
            push   ebx
            push   ecx
            push   edx
            
            call    heap_init
            
            mov     [bp-head], null     ; nodo* head = null
            mov     ebx, bp
            add     ebx, head           ; ebx = &head
            

            push    5
            call    new_nodo
            add     sp, 4

            push    eax
            push    ebx
            call    insert_nodo
            add     sp, 8

            ;mov     [ebx], eax

            push    10
            call    new_nodo
            add     sp, 4

            push    eax
            push    ebx
            call    insert_nodo
            add     sp, 8

            ;mov     ecx, [ebx]
            ;mov     [ecx+4], eax

            push    [ebx]
            call    printlist
            add     sp, 4

            pop     edx
            pop     ecx
            pop     ebx
            pop     eax
            mov     sp, bp
            pop     bp
            stop


;----------------------------------------
; imprime una lista de valores enteros
; parámentros: +8 puntero a primer nodo
;----------------------------------------
; invocación:
; push  <nodo*>
; call  list_print
; add   sp, 4
;----------------------------------------

printlist:  push    bp
            mov     bp, sp

            mov     eax, 1
            mov     ch, 4
            mov     cl, 1
            mov     edx, [bp+8]

printotro:  cmp     edx, null
            jz      printfin

            sys     2
            mov     edx, [edx+sig]

            jmp     printotro

printfin:   pop     edx
            pop     ecx
            pop     eax
            mov     sp, bp
            pop     bp
            ret           

