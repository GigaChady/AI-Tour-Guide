import uuid
from datetime import datetime, timezone
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy import Column, Float, String, Boolean, DateTime, ForeignKey, Index, Integer, ARRAY
from geoalchemy2 import Geometry # do zrobienia jakos potem, moze sie przyda do przechowywania trasy czy lokalizacji
from sqlalchemy.orm import relationship
from sqlalchemy import JSON
from sqlalchemy.orm import DeclarativeBase

class Base(DeclarativeBase):
    pass


def utc_now_naive() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String, unique=True, nullable=False)
    hashed_password = Column(String, nullable=True) # nullable for Google-auth users
    google_id = Column(String, unique=True, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=utc_now_naive)
    nazwa = Column(String, nullable=True) 
    imie = Column(String, nullable=True)
    nazwisko = Column(String, nullable=True)
    gender_option_id = Column(Integer, ForeignKey("demographics_gender_options.id"), nullable=True)
    gender_custom = Column(String, nullable=True)
    wiek = Column(Float, nullable=True)
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    gender_option = relationship("DemographicsGenderOption")
    routes = relationship("Route", back_populates="user")
    refresh_tokens = relationship("RefreshToken", back_populates="user")


class UserPreferences(Base):
    __tablename__ = "user_preferences"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), unique=True)
    interests = Column(JSON, default=list) # moze jakis enum albo cos zeby ograniczyc do konkretnych kategorii, ale na razie niech bedzie string

    user = relationship("User", back_populates="preferences")


class PreferenceQuestionDefinition(Base):
    __tablename__ = "preference_questions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    question_key = Column(String, unique=True, nullable=False)
    title = Column(String, nullable=False)
    type = Column(String, nullable=False)
    sort_order = Column(Integer, default=0, nullable=False)
    min_value = Column(Integer, nullable=True)
    max_value = Column(Integer, nullable=True)
    required = Column(Boolean, default=False, nullable=False)

    answers = relationship(
        "PreferenceQuestionOptionDefinition",
        back_populates="question",
        cascade="all, delete-orphan",
        order_by="PreferenceQuestionOptionDefinition.sort_order.asc()",
    )


class PreferenceQuestionOptionDefinition(Base):
    __tablename__ = "preference_question_options"

    id = Column(Integer, primary_key=True, autoincrement=True)
    question_id = Column(Integer, ForeignKey("preference_questions.id", ondelete="CASCADE"), nullable=False)
    answer_key = Column(String, nullable=False)
    title = Column(String, nullable=False)
    body = Column(String, nullable=True)
    trailing_content = Column(String, nullable=True)
    sort_order = Column(Integer, default=0, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)

    question = relationship("PreferenceQuestionDefinition", back_populates="answers")


class Route(Base):
    __tablename__ = "routes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    started_at = Column(DateTime, default=utc_now_naive)
    ended_at = Column(DateTime, nullable=True)
    city = Column(String, nullable=True)
    name = Column(String, nullable=True) # moze jakis tytul trasy czy cos, zeby latwiej bylo potem rozpoznac trasy usera, ale na razie niech bedzie nullable, bo moze niektorym userom bedzie sie chcialo to wypelniac a innym nie
    # moze jeszcze jakies pola typu aktualna lokalizacja czy cos
    path = Column(Geometry("LINESTRING"), nullable=True) # do zrobienia jakos potem, moze sie przyda do przechowywania trasy
    distance_m = Column(Float, nullable=True)

    user = relationship("User", back_populates="routes")

    __table_args__ = (
        Index("ix_routes_user_id", "user_id"),
    )


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    token_hash = Column(String, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    revoked = Column(Boolean, default=False)

    user = relationship("User", back_populates="refresh_tokens")

    __table_args__ = (
        Index("ix_refresh_tokens_user_id_token_hash", "user_id"),
    )


class DemographicsGenderOption(Base):
    __tablename__ = "demographics_gender_options"

    id = Column(Integer, primary_key=True, autoincrement=True)
    code = Column(String, unique=True, nullable=False)
    label = Column(String, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)
    sort_order = Column(Integer, default=0, nullable=False)

