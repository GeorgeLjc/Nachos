#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int mystrlen(char *buffer)    //similar like strleng() ,get the length of the char arry
{
    int i;
    for(i=0;i<500;i++)
    {
        if(buffer[i]==0)
            return i;
    }
    return -1;

}

void main(int argc, char *argv[])
{	
    int fd=0;
    char *filename = "aa.txt";
    int ByteNum;
    char *buffer = "Hello! This is the test for Task2.1.\n";
    char buffersize = mystrlen(buffer);
    char buf[40];    //testing for Create(char *)

    creat(filename);
    printf("Calling 'creat(filename)'... ");
    printf("  done!\n");    //testing for Open()

    fd = open(filename);
    printf("Calling 'fd = open(filename)'... done!\n");
    printf("Return value fd = %d", fd);
    
    printf("\n");    //testing for Write()
    write(fd, buffer, buffersize); 
    close(fd);
    printf("Calling 'Write(buffer, buffersize, fd)' ...done!\n");    //testing for Read()

    fd = open(filename);
    int i;
    ByteNum=read(fd, buf, 40);  

    printf("Calling 'Read(buf, 40, fd)' ... Done\n");
    printf("Begin to print the 40 Bytes content of a nachos file:\n");
    printf(buf);

    close(fd);
    //halt();
}
