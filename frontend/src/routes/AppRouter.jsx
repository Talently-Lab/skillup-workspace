import { Routes, Route } from 'react-router-dom'
import Layout from '../components/layout/Layout'
import Home from '../pages/Home'
import Login from '../pages/Login'
import Register from '../pages/Register'
import AdminDashboard from '../pages/AdminDashboard'
import CourseCatalog from '../pages/CourseCatalog'
import CourseDetail from '../pages/CourseDetail'

export default function AppRouter() {
  return (
    <Routes>
      {/* Las rutas de autenticación van afuera para que NO carguen el Navbar general */}
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Register />} />
      <Route path="/admin" element={<AdminDashboard />} />

      {/* El resto de la app que sí lleva el Navbar principal */}
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/cursos" element={<CourseCatalog />} />
        <Route path="/cursos/:id" element={<CourseDetail />} />
      </Route>
    </Routes>
  )
}