import { Routes, Route, Navigate } from "react-router-dom";
import Layout from "../components/layout/Layout";
import Courses from "../components/courses/Courses";
import CourseDetails from "../pages/CourseDetails";

export default function AppRouter() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/cursos" replace />} />
        <Route path="/cursos" element={<Courses />} />
        <Route path="/cursos/:id" element={<CourseDetails />} />
      </Route>
    </Routes>
  );
}
