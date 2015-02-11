/*
 * Kod pochodzi ze strony http://docs.gstreamer.com/display/GstSDK/Android+tutorial+3%3A+Video.
 * Elementem, który jest zmieniony jest budowa potoku (pipeline) w celu odebrania strumienia wideo.
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <gst/gst.h>
#include <gst/interfaces/xoverlay.h>
#include <gst/video/video.h>
#include <pthread.h>

GST_DEBUG_CATEGORY_STATIC (debug_category);
#define GST_CAT_DEFAULT debug_category

/*
 * These macros provide a way to store the native pointer to CustomData, which might be 32 or 64 bits, into
 * a jlong, which is always 64 bits, without warnings.
 */
#if GLIB_SIZEOF_VOID_P == 8
# define GET_CUSTOM_DATA(env, thiz, fieldID) (CustomData *)(*env)->GetLongField (env, thiz, fieldID)
# define SET_CUSTOM_DATA(env, thiz, fieldID, data) (*env)->SetLongField (env, thiz, fieldID, (jlong)data)
#else
# define GET_CUSTOM_DATA(env, thiz, fieldID) (CustomData *)(jint)(*env)->GetLongField (env, thiz, fieldID)
# define SET_CUSTOM_DATA(env, thiz, fieldID, data) (*env)->SetLongField (env, thiz, fieldID, (jlong)(jint)data)
#endif

/* Structure to contain all our information, so we can pass it to callbacks */
typedef struct _CustomData {
  jobject app;           /* Application instance, used to call its methods. A global reference is kept. */
  GstElement *pipeline;  /* The running pipeline */
  GMainContext *context; /* GLib context used to run the main loop */
  GMainLoop *main_loop;  /* GLib main loop */
  gboolean initialized;  /* To avoid informing the UI multiple times about the initialization */
  GstElement *video_sink; /* The video sink element which receives XOverlay commands */
  ANativeWindow *native_window; /* The Android native window where video will be rendered */
} CustomData;

/* These global variables cache values which are not changing during execution */
static pthread_t gst_app_thread;
static pthread_key_t current_jni_env;
static JavaVM *java_vm;
static jfieldID custom_data_field_id;
static jmethodID on_gstreamer_initialized_method_id;

/*
 * Private methods
 */

/* Register this thread with the VM */
static JNIEnv *attach_current_thread (void) {
  JNIEnv *env;
  JavaVMAttachArgs args;

  GST_DEBUG ("Attaching thread %p", g_thread_self ());
  args.version = JNI_VERSION_1_4;
  args.name = NULL;
  args.group = NULL;

  if ((*java_vm)->AttachCurrentThread (java_vm, &env, &args) < 0) {
    GST_ERROR ("Failed to attach current thread");
    return NULL;
  }

  return env;
}

/* Unregister this thread from the VM */
static void detach_current_thread (void *env) {
  GST_DEBUG ("Detaching thread %p", g_thread_self ());
  (*java_vm)->DetachCurrentThread (java_vm);
}

/* Retrieve the JNI environment for this thread */
static JNIEnv *get_jni_env (void) {
  JNIEnv *env;

  if ((env = pthread_getspecific (current_jni_env)) == NULL) {
    env = attach_current_thread ();
    pthread_setspecific (current_jni_env, env);
  }

  return env;
}



/* Check if all conditions are met to report GStreamer as initialized.
 * These conditions will change depending on the application */

static void check_initialization_complete (CustomData *data) {
	JNIEnv *env = get_jni_env ();
	  if (!data->initialized && data->native_window && data->main_loop) {
	    GST_DEBUG ("Initialization complete, notifying application. native_window:%p main_loop:%p", data->native_window, data->main_loop);

	   gst_x_overlay_set_window_handle (GST_X_OVERLAY (data->video_sink), (guintptr)data->native_window);

	    (*env)->CallVoidMethod (env, data->app, on_gstreamer_initialized_method_id);
	    if ((*env)->ExceptionCheck (env)) {
	      GST_ERROR ("Failed to call Java method");
	      (*env)->ExceptionClear (env);
	    }
	    data->initialized = TRUE;
  }
}

/* Main method for the native code. This is executed on its own thread. */
static void *app_function (void *userdata) {
  JavaVMAttachArgs args;
  GstBus *bus;
  CustomData *data = (CustomData *)userdata;
  GSource *bus_source;
  GError *error = NULL;

  GST_DEBUG ("Creating pipeline in CustomData at %p", data);

  /* Create our own GLib Main Context and make it the default one */
  data->context = g_main_context_new ();
  g_main_context_push_thread_default(data->context);

  /* Budowa potoku, umo¿liwaj¹ce odbieranie strumienowanego wideo. */
  data->pipeline = gst_parse_launch("udpsrc port=5000 caps=\"application/x-rtp, media=video, clock-rate=90000, encoding-name=H264, sprop-parameter-sets=\\\"J2QAH6wrQCIC3y8A8SJq\\\\,KO4CXLA\\\\=\\\"\", payload=96\” ! queue ! rtph264depay  ! ffdec_h264 ! autovideosink sync=false", &error);
   if (error) {
    gchar *message = g_strdup_printf("Unable to build pipeline: %s", error->message);
    g_clear_error (&error);
    g_free (message);
    return NULL;
  }
  /* Set the pipeline to READY, so it can already accept a window handle, if we have one */
    gst_element_set_state(data->pipeline, GST_STATE_READY);

    data->video_sink = gst_bin_get_by_interface(GST_BIN(data->pipeline), GST_TYPE_X_OVERLAY);
    if (!data->video_sink) {
      GST_ERROR ("Could not retrieve video sink");
      return NULL;
    }

  /* Instruct the bus to emit signals for each received message, and connect to the interesting signals */
  bus = gst_element_get_bus (data->pipeline);
  bus_source = gst_bus_create_watch (bus);
  g_source_set_callback (bus_source, (GSourceFunc) gst_bus_async_signal_func, NULL, NULL);
  g_source_attach (bus_source, data->context);
  g_source_unref (bus_source);
  gst_object_unref (bus);

  /* Create a GLib Main Loop and set it to run */
  GST_DEBUG ("Entering main loop... (CustomData:%p)", data);
  data->main_loop = g_main_loop_new (data->context, FALSE);
   check_initialization_complete (data);
  g_main_loop_run (data->main_loop);
  GST_DEBUG ("Exited main loop");
  g_main_loop_unref (data->main_loop);
  data->main_loop = NULL;

  /* Free resources */
  g_main_context_pop_thread_default(data->context);
  g_main_context_unref (data->context);
  gst_element_set_state (data->pipeline, GST_STATE_NULL);
  gst_object_unref (data->pipeline);

  return NULL;
}

/*
 * Java Bindings
 */

/* Instruct the native code to create its internal data structure, pipeline and thread */
static void gst_native_init (JNIEnv* env, jobject thiz) {
  CustomData *data = g_new0 (CustomData, 1);
  SET_CUSTOM_DATA (env, thiz, custom_data_field_id, data);
  GST_DEBUG_CATEGORY_INIT (debug_category, "StreamingC", 0, "StreamingClient");
  gst_debug_set_threshold_for_name("StreamingC", GST_LEVEL_DEBUG);
  GST_DEBUG ("Created CustomData at %p", data);
  data->app = (*env)->NewGlobalRef (env, thiz);
  GST_DEBUG ("Created GlobalRef for app object at %p", data->app);
  pthread_create (&gst_app_thread, NULL, &app_function, data);
}

/* Quit the main loop, remove the native thread and free resources */
static void gst_native_finalize (JNIEnv* env, jobject thiz) {
  CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
  if (!data) return;
  GST_DEBUG ("Quitting main loop...");
  g_main_loop_quit (data->main_loop);
  GST_DEBUG ("Waiting for thread to finish...");
  pthread_join (gst_app_thread, NULL);
  GST_DEBUG ("Deleting GlobalRef for app object at %p", data->app);
  (*env)->DeleteGlobalRef (env, data->app);
  GST_DEBUG ("Freeing CustomData at %p", data);
  g_free (data);
  SET_CUSTOM_DATA (env, thiz, custom_data_field_id, NULL);
  GST_DEBUG ("Done finalizing");
}

/* Set pipeline to PLAYING state */
static void gst_native_play (JNIEnv* env, jobject thiz) {
  CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
  if (!data) return;
  GST_DEBUG ("Setting state to PLAYING");
  gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
}

/* Set pipeline to PAUSED state */
static void gst_native_pause (JNIEnv* env, jobject thiz) {
  CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
  if (!data) return;
  GST_DEBUG ("Setting state to PAUSED");
  gst_element_set_state (data->pipeline, GST_STATE_PAUSED);
}

/* Statyczny inicjalizator klasy. Wyszukiwane s¹ identyfikatory metod i pól z kodu napisanego w javie,
 *  których póŸniej bêdzie potrzebowa³ kod napisany w C  */
static jboolean gst_native_class_init (JNIEnv* env, jclass klass) {
  custom_data_field_id = (*env)->GetFieldID (env, klass, "native_custom_data", "J");

  on_gstreamer_initialized_method_id = (*env)->GetMethodID (env, klass, "onGStreamerInitialized", "()V");

  if (!custom_data_field_id || !on_gstreamer_initialized_method_id) {
    /* We emit this message through the Android log instead of the GStreamer log because the later
     * has not been initialized yet.
     */
    __android_log_print (ANDROID_LOG_ERROR, "StreamingC", "The calling class does not implement all necessary interface methods");
    return JNI_FALSE;
  }
  return JNI_TRUE;

}
static void gst_native_surface_init (JNIEnv *env, jobject thiz, jobject surface) {
  CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
  if (!data) return;
  ANativeWindow *new_native_window = ANativeWindow_fromSurface(env, surface);
  GST_DEBUG ("Received surface %p (native window %p)", surface, new_native_window);

  if (data->native_window) {
    ANativeWindow_release (data->native_window);
    if (data->native_window == new_native_window) {
      GST_DEBUG ("New native window is the same as the previous one", data->native_window);
      if (data->video_sink) {
        gst_x_overlay_expose(GST_X_OVERLAY (data->video_sink));
        gst_x_overlay_expose(GST_X_OVERLAY (data->video_sink));
      }
      return;
    } else {
      GST_DEBUG ("Released previous native window %p", data->native_window);
      data->initialized = FALSE;
    }
  }
  data->native_window = new_native_window;

  check_initialization_complete (data);
}

static void gst_native_surface_finalize (JNIEnv *env, jobject thiz) {
  CustomData *data = GET_CUSTOM_DATA (env, thiz, custom_data_field_id);
  if (!data) return;
  GST_DEBUG ("Releasing Native Window %p", data->native_window);

  if (data->video_sink) {
    gst_x_overlay_set_window_handle (GST_X_OVERLAY (data->video_sink), (guintptr)NULL);
    gst_element_set_state (data->pipeline, GST_STATE_READY);
  }

  ANativeWindow_release (data->native_window);
  data->native_window = NULL;
  data->initialized = FALSE;
}


/* List of implemented native methods */
static JNINativeMethod native_methods[] = {
  { "nInit", "()V", (void *) gst_native_init},
  { "nFinalize", "()V", (void *) gst_native_finalize},
  { "nPlay", "()V", (void *) gst_native_play},
  { "nStop", "()V", (void *) gst_native_pause},
  { "nSurfaceInit", "(Ljava/lang/Object;)V", (void *) gst_native_surface_init},
  { "nSurfaceFinalize", "()V", (void *) gst_native_surface_finalize},
  { "nClassInit", "()Z", (void *) gst_native_class_init}
};

/* Library initializer */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv *env = NULL;

  java_vm = vm;

  if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
    __android_log_print (ANDROID_LOG_ERROR, "StreamingC", "Could not retrieve JNIEnv");
    return 0;
  }
  jclass klass = (*env)->FindClass (env, "com/ww/streamingclient/StreamingClient");
  (*env)->RegisterNatives (env, klass, native_methods, G_N_ELEMENTS(native_methods));

  pthread_key_create (&current_jni_env, detach_current_thread);

  return JNI_VERSION_1_4;
}
