// A tiny, original DOS .COM program for the native regression test. No game data.
.code16
.text
.global _start
_start:
    movw $0x13, %ax
    int $0x10
    movw $0xa000, %ax
    movw %ax, %es
    cld
animate:
    xorb %ah, %ah
    int $0x1a
    movb %dl, %al
    xorw %di, %di
    movw $64000, %cx
    rep stosb
    movb $1, %ah
    int $0x16
    jz animate
    xorb %ah, %ah
    int $0x16
    cmpb $27, %al
    jne animate
    movw $0x4c00, %ax
    int $0x21
