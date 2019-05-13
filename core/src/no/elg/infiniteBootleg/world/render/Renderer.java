package no.elg.infiniteBootleg.world.render;

/**
 * @author Elg
 */
public interface Renderer {

    /**
     * Update the displaying frame, might be called every frame.
     */
    void update();

    /**
     * Render the frame. Called every frame.
     */
    void render();
}
