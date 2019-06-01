/**
 * 
 */
package Bsnmp;

/**
 * @author Laptop
 *
 */
public class TimerComponentes extends Thread
{
    private int tiempo = 6;

    /***
     * Inicia un Timer en un hilo el cual durara el tiempo establecido
     * 
     * @param tiempo
     *            en segundos
     */
    public TimerComponentes(int tiempo)
    {
        // TODO Auto-generated constructor stub
        this.tiempo = tiempo * 1000;
    }

    @Override
    public void run()
    {
        try
        {
            timeRunning();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /***
     * Inicia el contador para que finalize el tiempo
     * 
     * @throws InterruptedException
     */
    private void timeRunning() throws InterruptedException
    {
        Thread.sleep(tiempo);
    }
}
