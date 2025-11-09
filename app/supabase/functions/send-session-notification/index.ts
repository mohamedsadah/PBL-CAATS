import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import * as admin from "npm:firebase-admin@11.11.1/app";
import { getMessaging } from "npm:firebase-admin@11.11.1/messaging";

try {
  admin.initializeApp({
    credential: admin.cert({
      projectId: Deno.env.get("FIREBASE_PROJECT_ID"),
      clientEmail: Deno.env.get("FIREBASE_CLIENT_EMAIL"),
      privateKey: Deno.env.get("FIREBASE_PRIVATE_KEY")?.replace(/\\n/g, "\n"),
    }),
  });
} catch (e) {
  console.log("Firebase already initialized");
}


Deno.serve(async (req) => {
  // 1. Get the new session data from the request body
  const { record: newSession } = await req.json();
  const { section_id, course_id } = newSession;

  // 2. Create a Supabase client to query for data
  const supabaseClient = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
  );

  // 3. Get the course title for the notification message
  const { data: courseData } = await supabaseClient
    .from("courses")
    .select("title")
    .eq("id", course_id)
    .single();
  const courseTitle = courseData?.title || "A new session";

  // 4. Find all students in the relevant section and get their FCM tokens
  const { data: students, error } = await supabaseClient
    .from("students")
    .select("fcm_token")
    .eq("section_id", section_id);

  if (error || !students || students.length === 0) {
    console.error("No students found for this section or error:", error);
    return new Response("No students found", { status: 200 });
  }

  // Filter out any null/empty tokens
  const tokens = students.map((s) => s.fcm_token).filter(Boolean);

  if (tokens.length === 0) {
    return new Response("No valid FCM tokens found", { status: 200 });
  }

  // 5. Construct the push notification payload
  const message = {
    notification: {
      title: "New Session Started!",
      body: `${courseTitle} has just started. Tap to mark your attendance.`,
    },
    tokens: tokens, // Use sendToDevice for multiple tokens
  };

  // 6. Send the message using Firebase Admin SDK
  try {
    const response = await getMessaging().sendMulticast(message);
    console.log("Successfully sent message:", response);
    return new Response(JSON.stringify(response), {
      headers: { "Content-Type": "application/json" },
      status: 200,
    });
  } catch (e) {
    console.error("Error sending message:", e);
    return new Response(String(e), { status: 500 });
  }
});