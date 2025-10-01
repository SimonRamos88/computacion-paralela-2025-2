package co.edu.unal.paralela;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

/**
 * Clase que contiene los métodos para implementar la suma de los recíprocos de un arreglo usando paralelismo.
 */
public final class ReciprocalArraySum {

    /**
     * Pool compartido para evitar overhead de creación/destrucción
     */
    private static final ForkJoinPool SHARED_POOL = new ForkJoinPool();

    /**
     * Constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Calcula secuencialmente la suma de valores recíprocos para un arreglo.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Calcula la suma de los recíprocos de los elementos del arreglo
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * calcula el tamaño de cada trozo o sección, de acuerdo con el número de secciones para crear
     * a través de un número dado de elementos.
     *
     * @param nChunks El número de secciones (chunks) para crear
     * @param nElements El número de elementos para dividir
     * @return El tamaño por defecto de la sección (chunk)
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Función techo entera
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Calcula el índice del elemento inclusivo donde la sección/trozo (chunk) inicia,
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk la sección/trozo (chunk) para cacular la posición de inicio
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice inclusivo donde esta sección/trozo (chunk) inicia en el conjunto de 
     *         nElements
     */
    private static int getChunkStartInclusive(final int chunk,
            final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Calcula el índice del elemento exclusivo que es proporcionado al final de la sección/trozo (chunk),
     * dado que hay cierto número de secciones/trozos (chunks).
     *
     * @param chunk La sección para calcular donde termina
     * @param nChunks Cantidad de secciones/trozos (chunks) creados
     * @param nElements La cantidad de elementos de la sección/trozo que deben atravesarse
     * @return El índice de terminación exclusivo para esta sección/trozo (chunk)
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * Este pedazo de clase puede ser completada para para implementar el cuerpo de cada tarea creada
     * para realizar la suma de los recíprocos del arreglo en paralelo.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Umbral secuencial para evitar sobrecarga de paralelización
         */
        private static final int SEQUENTIAL_THRESHOLD = 5000;
        
        /**
         * Iniciar el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int startIndexInclusive;
        /**
         * Concluir el índice para el recorrido transversal hecho por esta tarea.
         */
        private final int endIndexExclusive;
        /**
         * Arreglo de entrada para la suma de recíprocos.
         */
        private final double[] input;
        /**
         * Valor intermedio producido por esta tarea.
         */
        private double value;

        /**
         * Constructor.
         * @param setStartIndexInclusive establece el índice inicial para comenzar
         *        el recorrido trasversal.
         * @param setEndIndexExclusive establece el índice final para el recorrido trasversal.
         * @param setInput Valores de entrada
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Adquiere el valor calculado por esta tarea.
         * @return El valor calculado por esta tarea
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            // Si el rango es pequeño, calcular secuencialmente
            if (endIndexExclusive - startIndexInclusive <= SEQUENTIAL_THRESHOLD) {
                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }
            } else {
                // Dividir recursivamente en dos mitades
                int midpoint = (startIndexInclusive + endIndexExclusive) / 2;
                ReciprocalArraySumTask leftTask = new ReciprocalArraySumTask(startIndexInclusive, midpoint, input);
                ReciprocalArraySumTask rightTask = new ReciprocalArraySumTask(midpoint, endIndexExclusive, input);
                
                // Ejecutar en paralelo usando fork/join
                leftTask.fork();
                rightTask.compute();
                leftTask.join();
                
                // Combinar resultados
                value = leftTask.getValue() + rightTask.getValue();
            }
        }
    }

    /**
     * Para hacer: Modificar este método para calcular la misma suma de recíprocos como le realizada en
     * seqArraySum, pero utilizando dos tareas ejecutándose en paralelo dentro del framework ForkJoin de Java
     * Se puede asumir que el largo del arreglo de entrada 
     * es igualmente divisible por 2.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        // Creamos un pool dedicado con 2 hilos para máximo paralelismo
        ForkJoinPool pool = new ForkJoinPool(2);
        
        try {
            // Calculamos el punto medio para dividir el arreglo en dos
            int midpoint = input.length / 2;
            
            // Creamos dos tareas para las dos mitades
            ReciprocalArraySumTask leftTask = new ReciprocalArraySumTask(0, midpoint, input);
            ReciprocalArraySumTask rightTask = new ReciprocalArraySumTask(midpoint, input.length, input);
            
            // Ejecutamos las tareas en paralelo usando fork/join
            leftTask.fork();
            rightTask.compute();
            leftTask.join();
            
            // Combinamos los resultados
            double sum = leftTask.getValue() + rightTask.getValue();
            
            return sum;
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Para hacer: extender el trabajo hecho para implementar parArraySum que permita utilizar un número establecido
     * de tareas para calcular la suma del arreglo recíproco. 
     * getChunkStartInclusive y getChunkEndExclusive pueden ser útiles para cacular 
     * el rango de elementos índice que pertenecen a cada sección/trozo (chunk).
     *
     * @param input Arreglo de entrada
     * @param numTasks El número de tareas para crear
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parManyTaskArraySum(final double[] input,
            final int numTasks) {
        // Creamos un pool dedicado con el número de tareas para máximo paralelismo
        ForkJoinPool pool = new ForkJoinPool(numTasks);
        
        try {
            // Creamos exactamente numTasks tareas usando los métodos helper
            ReciprocalArraySumTask[] tasks = new ReciprocalArraySumTask[numTasks];
            
            for (int i = 0; i < numTasks; i++) {
                int start = getChunkStartInclusive(i, numTasks, input.length);
                int end = getChunkEndExclusive(i, numTasks, input.length);
                tasks[i] = new ReciprocalArraySumTask(start, end, input);
            }
            
            // Ejecutamos todas las tareas en paralelo
            for (int i = 0; i < numTasks; i++) {
                tasks[i].fork();
            }
            
            // Esperamos a que todas las tareas terminen
            for (int i = 0; i < numTasks; i++) {
                tasks[i].join();
            }
            
            // Combinamos los resultados
            double sum = 0;
            for (ReciprocalArraySumTask task : tasks) {
                sum += task.getValue();
            }
            
            return sum;
        } finally {
            pool.shutdown();
        }
    }
}
