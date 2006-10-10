CCOMPILE = gcc
COMPILEOPTION = -c
INCLUDEDIR =-I./ 
LINK = gcc
LINKOPTION = -o Test
OBJS = multi_http_send.o multi_http_send_test.o
OUTPUT = Test
$(OUTPUT): $(OBJS) 
	$(LINK) $(LINKOPTION)  $(OBJS)

clean: 
	rm -f $(OBJS)
	rm -f $(OUTPUT)

all: clean $(OUTPUT)

.SUFFIXES:  .c .o
.c.o:
	$(CCOMPILE) -c -o $*.o $(COMPILEOPTION) $(INCLUDEDIR) $*.c
